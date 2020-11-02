package com.zhanghe.pool.core.connection;

import com.zhanghe.pool.core.collection.concurrent.ConcurrentBorrowBag;
import com.zhanghe.pool.core.collection.state.UseState;
import com.zhanghe.pool.core.config.DataSourceConfig;
import com.zhanghe.pool.core.connection.exception.PoolConnectionDepleteException;
import com.zhanghe.pool.core.connection.exception.PoolShutdownException;
import com.zhanghe.pool.core.connection.factory.ConnectionFactory;
import com.zhanghe.pool.core.connection.recycle.RecycleStrategy;
import com.zhanghe.pool.core.util.date.CLOCKHelp;
import com.zhanghe.pool.core.util.lock.EmptySemaphore;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * 通用连接池 -如果要实习自己的连接池，继承实习部分方法即可
 * <p>
 * 标准的模板模式的设计思想
 * <p>
 * 连接池池的启发=hikari 代码简单明了 而且速度极快 spring boot 数据库连接池自带的
 * <p>
 * 运用了hikari 的部分核心思路
 *
 * @author: ZhangHe
 * @since: 2020/10/21 14:59
 */
@Slf4j
public class GeneralPool<T> extends AbstractConnectionPool<T> {

    /**
     * 调度程序非常重要
     * 对于连接到达存活时间进行回收
     * 对于最小空闲连接的补充
     */
    protected ScheduledExecutorService scheduledExecutorService;

    //添加连接服务
    protected ExecutorService addConnectionService;

    //关闭连接服务
    protected ExecutorService closeConnectionService;

    //超时回收策略
    protected RecycleStrategy recycleStrategy;

    //信号数
    Semaphore semaphore = new EmptySemaphore();

    /**
     * 生成一个正常的连接
     */
    protected ConnectionFactory<T> connectionFactory;

    //添加连接视图 这个是受到 Hikari的启发 =>来自于addTaskQueue
    protected Collection<Runnable> addConnView = Collections.unmodifiableCollection(addTaskQueue);

    /**
     * 定时任务
     * 定时添加最小连接数
     * 定时清理空闲连接
     */
    protected Runnable timedTask;

    /**
     * 连接袋  虽然继承与 阻塞队列已实现
     * 但是 poll 并不是真正弹出去连接 而是修改状态为使用状态
     * 也就是你必须调用remove方法来确保真正弹出
     */
    ConcurrentBorrowBag<PoolEntity<T>> concurrentBorrowBag;


    public GeneralPool(DataSourceConfig dataSourceConfig,
                       ConnectionFactory connectionFactory,
                       ScheduledExecutorService scheduledExecutorService,
                       ExecutorService addConnectionService,
                       ExecutorService closeConnectionService,
                       ConcurrentBorrowBag concurrentBorrowBag,
                       RecycleStrategy recycleStrategy,
                       Runnable timedTask
    ) {
        super(dataSourceConfig);
        this.connectionFactory = connectionFactory;
        this.scheduledExecutorService = Optional.ofNullable(scheduledExecutorService).orElseGet(this::defaultScheduled);
        this.addConnectionService = Optional.ofNullable(addConnectionService).orElseGet(this::defaultAddService);
        this.closeConnectionService = Optional.ofNullable(closeConnectionService).orElseGet(this::defaultCloseService);
        //同步连接借用袋
        this.concurrentBorrowBag = Optional.ofNullable(concurrentBorrowBag).orElseGet(this::initBorrowBag);
        this.recycleStrategy = Optional.ofNullable(recycleStrategy).orElseGet(this::defaultRecycle); //回收策略
        this.timedTask = Optional.ofNullable(timedTask).orElseGet(this::defaultTimedTask); //启动定时任务
        this.starTask();
    }


    public GeneralPool(DataSourceConfig dataSourceConfig, ConnectionFactory connectionFactory) {
        this(dataSourceConfig, connectionFactory, null, null, null, null, null, null);
    }

    /**
     * 默认调度任务
     * 检查超出最小空闲连接的用来关闭
     * 保持最小空闲连接数
     * @return 默认的调度任务
     */
    public Runnable defaultTimedTask() {
        return () -> {
            DataSourceConfig config = getDataSourceConfig();

            //过滤出未使用的连接
            Collection<PoolEntity<T>> uns = concurrentBorrowBag.filter(UseState.UN_USE);

            //超出最小空闲连接 进行回收
            if (config.getIdleTimeout() > 0 && config.getMinIdle() < uns.size()) {
                int count = uns.size() - config.getMinIdle();
                uns.stream().limit(count).forEach(p -> {
                    /*
                     * 默认回收策略是彻底关闭连接，并且删除连接袋里面的连接
                     * 当然你也可以自定义的拉
                     */
                    closeConnectionService.submit(() -> recycleStrategy.recycle(p));
                });
            }
            //填充池
            fillPool();
        };
    }

    /**
     * 填充最小连接池
     */
    protected void fillPool() {

        //最大数量判断
        if (concurrentBorrowBag.size() < getDataSourceConfig().getMaxPoolSize()
                &&
                //小于最小空闲数判断
                concurrentBorrowBag.filter(UseState.UN_USE).size() < getDataSourceConfig().getMinIdle()
        ) {
            addConnectionService.submit(this::addConnection);
        }
    }

    /**
     * 默认策略为关闭连接 并且彻底丢弃
     *
     * @return 默认回收策略
     */
    public RecycleStrategy<PoolEntity<T>> defaultRecycle() {
        return (p) -> {
            if (p.compareSetState(UseState.UN_USE, UseState.RETAIN)) {
                //关闭连接
                closeConnection(p);
                return true;
            }
            return false;
        };
    }

    public void starTask() {
        this.config();
        this.init();
        scheduledExecutorService.scheduleWithFixedDelay(timedTask, 1, 30, TimeUnit.SECONDS);
    }

    private ConcurrentBorrowBag<PoolEntity<T>> initBorrowBag() {
        return new ConcurrentBorrowBag((waiterCount) -> {
            //如果连接状态确实少于需求量
            if (addConnView.size() < waiterCount && concurrentBorrowBag.size() < getDataSourceConfig().getMaxPoolSize()) {
                addConnectionService.submit(this::addConnection);
            }
        });
    }

    /**
     * 配置
     */
    protected void config() {

    }

    /**
     * 初始化
     */
    protected void init() {

    }

    /**
     * 属性验证
     */
    protected void validate() {

    }


    protected T getProxyConnection(PoolEntity<T> poolEntity) {
        throw new UnsupportedOperationException();
    }

    @Override
    BlockingQueue<PoolEntity<T>> connectionQueue() {
        return concurrentBorrowBag;
    }


    @SneakyThrows
    @Override
    public PoolEntity<T> createPoolEntity() {
        long maxLifetime = getDataSourceConfig().getMaxLifetime();
        PoolEntity<T> poolEntity = new PoolEntity<>(connectionFactory.getConnection(), this);
        //设置回收策略
        poolEntity.setFailureDiscard(scheduledExecutorService.schedule(() -> recycleStrategy.recycle(poolEntity), maxLifetime, TimeUnit.MILLISECONDS));
        return poolEntity;
    }

    @Override
    protected boolean isConnectionAlive(T con) {
        throw new UnsupportedOperationException();
    }

    protected boolean validateConnection(T con, long timeout) throws TimeoutException {
        try {
            return closeConnectionService.submit(() -> isConnectionAlive(con))
                    .get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error("Verify that the connection is broken");
        } catch (ExecutionException e) {
            log.error("execution exception", e);
        }
        return false;
    }

    @Override
    protected void recycle(PoolEntity<T> entity) {
        Optional.ofNullable(entity)
                .ifPresent(concurrentBorrowBag::requite);
    }


    @Override
    protected void closeConnection(PoolEntity<T> conn) {
        try {
            T connection = conn.close();
            if (connection instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) connection).close();
                } catch (Exception e) {
                    log.error("close connection fail", e);
                }
            }
            concurrentBorrowBag.remove(conn);
        } finally {
            decrementCount();
        }
    }

    void quietClose(PoolEntity<T> conn) {
        closeConnectionService.submit(() -> closeConnection(conn));
    }

    @Override
    public void shutdown() throws PoolShutdownException {

        if (status() == SHUTDOWN) {
            return;
        }
        //set shutdown status
        int c;
        do {
            c = ctl.get();
        } while (!ctl.compareAndSet(c, ctlOf(SHUTDOWN, count(c))));

        try {
            //close thread Pool
            scheduledExecutorService.shutdown();
            addConnectionService.shutdown();

            //close remaining connection
            concurrentBorrowBag.forEach(this::quietClose);
        } catch (Exception e) {
            throw new PoolShutdownException(e);
        }
    }

    @Override
    public T getConnectionObject() throws Exception {
        if (!isRunnable()) {
            throw new IllegalStateException("pool is closed");
        }

        try {
            //控制获取连接人数
            semaphore.acquire();

            long startTime = System.currentTimeMillis();

            PoolEntity<T> entity;

            DataSourceConfig config = getDataSourceConfig();

            long connectionTimeout = config.getConnectionTimeout();

            do {

                //指定超时时间获取
                entity = concurrentBorrowBag.poll(connectionTimeout, TimeUnit.MILLISECONDS);

                //数据源没有连接了
                if (Objects.isNull(entity)) {
                    log.error("The connection pool is not connected");
                    throw new PoolConnectionDepleteException();
                } else if (validateConnection(entity.getConnection(), config.getValidationTimeout())) {
                    //验证连接成功即可获取
                    return getProxyConnection(entity);
                }

                //没有验证成功下次接着获取
                connectionTimeout -= CLOCKHelp.elapseMillis(startTime);

            } while (connectionTimeout > 0L);

            throw new TimeoutException();
            /*
             *  代理连接请额外处理
             *  比如代理关闭方法,回收到连接池内
             */
        } finally {
            semaphore.release();
        }
    }


}
