package com.zhanghe.pool.core.connection;

import com.zhanghe.pool.core.config.DataSourceConfig;
import com.zhanghe.pool.core.connection.exception.PoolShutdownException;
import com.zhanghe.pool.core.util.thread.ThreadHelp;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * 连接池模板
 *
 * @author: ZhangHe
 * @since: 2020/10/20 15:24
 */
public abstract class AbstractConnectionPool<T> extends PoolBase implements ConnectionPool<T> {


    /**
     * 默认提供的线程工厂
     */
    protected ThreadFactory threadFactory;

    /**
     * 添加连接任务队列
     */
    protected BlockingQueue<Runnable> addTaskQueue;


    /**
     * 数据源配置
     */
    private DataSourceConfig dataSourceConfig;


    public AbstractConnectionPool(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
        //验证配置后 下面开始初始化
        validate();
        this.threadFactory = ThreadHelp.getThreadFactory(dataSourceConfig.getPoolName(), false);
        this.addTaskQueue = new ArrayBlockingQueue<>(dataSourceConfig.getMaxPoolSize());
    }

    /**
     * 验证属性
     */
    protected abstract void validate();

    /**
     * 获取数据源配置
     *
     * @return bridge config datasource info
     */
    protected DataSourceConfig getDataSourceConfig() {
        return this.dataSourceConfig;
    }


    ExecutorService defaultCloseService() {
        return ThreadHelp.singleCoreThreadPool(new LinkedBlockingQueue(getDataSourceConfig().getMaxPoolSize()), threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 默认添加服务
     *
     * @return 任何添加连接任务的执行器
     */
    ExecutorService defaultAddService() {
        //这里注意拒绝策略，由于最大连接池的限制问题 所以采用丢弃最早策略
        return ThreadHelp.singleCoreThreadPool(addTaskQueue, threadFactory, new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    /**
     * 默认推荐的调度线程池服务
     *
     * @return 返回调度线程池执行器
     */
    ScheduledThreadPoolExecutor defaultScheduled() {
        /*
         * 为什么这样设置？您必须足够了解线程池的工作原理
         * 调度线程池默认采用的阻塞队列是自带的 DelayedWorkQueue
         *  DelayedWorkQueue 翻译过来就是 延迟队列的意思
         * 储存的是 RunnableScheduledFuture 调度任务 的数组 并且超过数量会扩容
         * 只不过注意的是，存放的RunnableScheduledFuture 会进行堆排序，也就是快运行的的任务在顶部
         * 比较有意思的是 通过task取任务的时候，如果取的栈顶的任务还没有到 运行的时间 会让 leader 赋值
         * 已达到其他线程task 阻塞的地步。当然你还得详细考虑并发同时取的同一个栈顶的情况 看看jdk如何处理的
         * 调度核心线程数为1 一个个处理快到时间的任务足以
         * 非常棒的是，jdk 控制循环调度是在ScheduledFutureTask 这里面reExecutePeriodic 重新加载延迟队列中运行的
         */
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
                new ScheduledThreadPoolExecutor(1, threadFactory,
                        new ThreadPoolExecutor.DiscardPolicy());

        //shutdown 后不在运行定期任务
        scheduledThreadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        //并且把定期任务从延迟队列中删除
        scheduledThreadPoolExecutor.setRemoveOnCancelPolicy(true);
        return scheduledThreadPoolExecutor;
    }

    @Override
    public boolean addConnection() {
        if (isRunnable()) {
            //添加 连接
            Optional.ofNullable(createPoolEntity()).ifPresent(conn -> {
                connectionQueue().add(conn);
                //添加过的连接数量
                incrementCount();
            });
            return true;
        }
        return false;
    }

    /**
     * 通过连接队列去进行添加连接
     *
     * @return 返回自定义的队列
     */
    abstract Queue<PoolEntity<T>> connectionQueue();

    /**
     * 创建连接池实体
     *
     * @return 连接池实体
     */
    protected abstract PoolEntity<T> createPoolEntity();

    /**
     * 验证连接是否有效
     *
     * @param con 连接
     * @return 返回连接验证成功标识
     */
    protected abstract boolean isConnectionAlive(T con);

    /**
     * 回收连接池实体
     *
     * @param entity 连接池实体
     */
    protected abstract void recycle(PoolEntity<T> entity);

    @Override
    public void close() throws PoolShutdownException {
        shutdown();
    }


    /**
     * 关闭连接池中的连接
     *
     * @param conn 连接
     */
    protected abstract void closeConnection(PoolEntity<T> conn);

}
