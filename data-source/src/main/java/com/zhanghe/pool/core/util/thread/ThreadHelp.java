package com.zhanghe.pool.core.util.thread;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程中的帮助器
 * @author: ZhangHe
 * @since: 2020/10/21 15:16
 */
public abstract class ThreadHelp {

    public static void sleep(long mills,TimeUnit timeUnit){
        try {
            timeUnit.sleep(mills);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ThreadInterruptedInfo(String.format("线程已被中断:被中断的线程名称%s",
                    Thread.currentThread().getName()));
        }
    }

    public static ThreadFactory getThreadFactory(String namePrefix, boolean daemon){
        return new DataSourcePoolThreadFactory(namePrefix, daemon);
    }

    /**
     * 总归要点版权的
     * @return 默认线程工厂
     */
    public static ThreadFactory defaultThreadFactory(){
        return  getThreadFactory("zh-pool", false);
    }

    /**
     * 默认添加服务
     * @return 任何添加连接任务的执行器
     */
    public static ExecutorService singleCoreThreadPool(final BlockingQueue blockingQueue,final ThreadFactory threadFactory,final RejectedExecutionHandler policy) {
        //只有一个核心worker 线程执行  并且5秒中取不到任务就取消阻塞
        ThreadPoolExecutor executorService = new ThreadPoolExecutor(1,
                1,
                5,
                TimeUnit.SECONDS,
                blockingQueue,
                threadFactory,
                policy);
        executorService.allowCoreThreadTimeOut(true);
        return executorService;
    }

    protected static class DataSourcePoolThreadFactory implements ThreadFactory {
        //池数量
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        //线程组
        private final ThreadGroup group;
        //线程数量
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        //名称前缀
        private final String namePrefix;

        private boolean isDaemon;

        DataSourcePoolThreadFactory(String namePrefix, boolean daemon) {
            //获取安全管理器
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix;
            this.isDaemon = daemon;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon() && !isDaemon)
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
