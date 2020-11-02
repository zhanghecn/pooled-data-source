package com.zhanghe.pool.core.collection.concurrent;

import com.zhanghe.pool.core.collection.state.StateHandler;
import com.zhanghe.pool.core.collection.state.UseState;
import com.zhanghe.pool.core.util.date.CLOCKHelp;
import com.zhanghe.pool.core.util.thread.ThreadHelp;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 阻塞借用袋
 * from Hikari Concurrent Bag enlighten
 *
 * @author: ZhangHe
 * @since: 2020/10/22 14:41
 */
@Slf4j
public class ConcurrentBorrowBag<T extends UseState> extends AbstractQueue<T> implements BlockingQueue<T> {

    //真正存放的数据
    private CopyOnWriteArrayList<T> shareList;

    /**
     * 此同步队列是为了让阻塞的线程及时获取连接
     */
    private SynchronousQueue<T> handlerQueue;

    /**
     * 对于同一个线程内的连接。
     * 如sql 情况: 在一个方法内 获取连接关闭 再次获取成不一样的连接？如何保证回滚机制？
     * 但是一般框架(mybatis jpa)都只是一个连接操作一个线程所有sql
     * 这个local 存放不止1个。恐怕是怕什么骚操作，用的不是框架 一个线程内连接没有关闭就又取一个
     */
    private ThreadLocal<List<T>> local = new ThreadLocal<List<T>>() {
        @Override
        protected List<T> initialValue() {
            return new LinkedList<T>();
        }
    };

    StateHandler stateHandler;

    public ConcurrentBorrowBag(StateHandler stateHandler) {
        this.shareList = new CopyOnWriteArrayList<>();
        this.handlerQueue = new SynchronousQueue<>(true);
        this.stateHandler = stateHandler;
    }

    public void clear() {
        shareList = null;
        handlerQueue.clear();
        local = null;
    }

    public Collection<T> filter(int state) {
        return shareList.stream().filter(t -> t.getState() == state).collect(Collectors.toList());
    }


    /**
     * 有多少个线程等待着获取连接
     */
    private AtomicInteger waiter = new AtomicInteger();

    /**
     * 全部迭代器 ？会不会只包含未使用的会更好点？
     *
     * @return 全部连接迭代器 包含已经被使用过了的
     */
    @Override
    public Iterator<T> iterator() {
        return shareList.iterator();
    }

    /**
     * 连接袋的连接的数量，经供参考
     *
     * @return 连接袋里面的数量
     */
    @Override
    public int size() {
        return shareList.size();
    }

    @Override
    public boolean offer(T t) {
        shareList.add(t);
        //存在等待连接的人 从handlerQueue 取出
        while (waiter.get() > 0 && !handlerQueue.offer(t)) {
            Thread.yield();
        }
        return true;
    }

    @Override
    public boolean remove(Object o) {
        T t = (T) o;
        //只有没有被使用过的才能删除
        if (o == null || (!t.compareSetState(UseState.UN_USE, UseState.REMOVE) &&
                !t.compareSetState(UseState.RETAIN, UseState.REMOVE))
        ) {
            log.debug("remove conn fail");
            return false;
        }
        //删除连接
        local.get().remove(o);
        return shareList.remove(o);
    }

    /**
     * 删除一个没有被使用过的连接
     * 不过并不建议使用
     *
     * @return 返回删除的连接
     */
    @Override
    public T remove() {
        return shareList.stream()
                .filter(t -> t.compareSetState(UseState.UN_USE, UseState.RETAIN))
                .peek(this::remove)
                .findAny().orElse(null);
    }


    /**
     * 获取一个没有被使用过的连接
     *
     * @return 返回连接
     */
    @Override
    public T poll() {
        return shareList.stream()
                .filter(t -> t.compareSetState(UseState.UN_USE, UseState.USE))
                .findAny()
                .orElse(null)
                ;
    }

    /**
     * 注意此方法获取后也会改成使用状态
     *
     * @return 参考poll
     */
    @Override
    public T element() {
        return Optional.ofNullable(poll()).orElseThrow(NoSuchElementException::new);
    }

    @Override
    public T peek() {
        return poll();
    }

    @Override
    public void put(T t) throws InterruptedException {
        throw new UnsupportedOperationException("不建议长期阻塞");
    }

    /**
     * 超时时间添加连接 推荐使用
     *
     * @param t       添加的连接
     * @param timeout 超时时间
     * @param unit    超时单位
     * @return 是否在指定时间内添加成功
     * @throws InterruptedException 指定阻塞时间内添加 突然中断异常
     */
    @Override
    public boolean offer(T t, long timeout, TimeUnit unit) throws InterruptedException {
        return handlerQueue.offer(t, timeout, unit) && shareList.add(t);
    }


    @Override
    public T take() throws InterruptedException {
        throw new UnsupportedOperationException("不建议没有超时时间获取");
    }


    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        try {
            T poll;
            int waiterCount = this.waiter.incrementAndGet();
            if (Objects.isNull(poll = poll())) {
                stateHandler.execute(waiterCount);
                long start = System.currentTimeMillis();

                long timeoutM = unit.toMillis(timeout);
                do {
                    //指定超时阻塞时间获取  当然可能直接获取到了
                    poll = Optional.ofNullable(handlerQueue.poll(timeout, unit))
                            .filter(c -> c.compareSetState(UseState.UN_USE, UseState.USE))
                            .orElse(null);
                    if (Objects.nonNull(poll)) {
                        return poll;
                    }
                    //对于被使用的重新计算剩下的超时时间
                    timeoutM -= CLOCKHelp.elapseMillis(start);
                    start = System.currentTimeMillis();
                } while (timeoutM > 0); //超时时间还够用接着下次获取
            }
            return poll;
        } finally {
            //连接获取完毕就减去等待的人
            this.waiter.decrementAndGet();
        }
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }


    @Override
    public int drainTo(Collection<? super T> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        int min = Math.min(maxElements, shareList.size());
        shareList.stream().limit(min).collect(Collectors.toCollection(() -> c));
        return min;
    }

    /**
     * 偿还
     *
     * @param bag 归还的元素
     */
    public void requite(T bag) {
        //归还设置成未使用
        bag.setState(UseState.UN_USE);
        int c;
        //有其他线程非常急需连接
        while ((c = waiter.get()) > 0) {
            //突然被其他线程使用到了 或者 队列添加成功
            if (bag.getState() != UseState.UN_USE || handlerQueue.offer(bag)) {
                return;
            }
            // greater equal 255
            if ((c & 0xff) == 0xff) {
                ThreadHelp.sleep(100, TimeUnit.MILLISECONDS);
            }
            Thread.yield();
        }

        local.get().add(bag);
    }


    public boolean reserve(T t) {
        return t.compareSetState(UseState.UN_USE, UseState.RETAIN);
    }
}
