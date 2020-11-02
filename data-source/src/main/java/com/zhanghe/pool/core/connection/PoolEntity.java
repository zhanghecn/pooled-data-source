package com.zhanghe.pool.core.connection;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 连接池存放的实体对象 建议在去继承这个类，来到达最终适配你的连接池里面的元素对象
 *
 * @author: ZhangHe
 * @since: 2020/10/20 16:58
 */
@Data
@Slf4j
public class PoolEntity<T> implements PoolObject<T> {

    //最后访问时间
    private long lastAccessed = System.currentTimeMillis();

    //连接
    private T connection;

    //状态
    protected AtomicInteger status = new AtomicInteger(UN_USE);

    protected ConnectionPool<T> connectionPool;

    //失效执行丢弃任务
    private ScheduledFuture failureDiscard;

    public PoolEntity(T connection, ConnectionPool<T> connectionPool) {
        this.connection = connection;
        this.connectionPool = connectionPool;
    }

    @Override
    public int getState() {
        return status.get();
    }

    @Override
    public void setState(int u) {
        status.set(u);
    }

    @Override
    public boolean compareSetState(int expect, int update) {
        return status.compareAndSet(expect, update);
    }

    @Override
    public void recycle(long lastAccessed) {
        this.lastAccessed = lastAccessed;
        if(connectionPool instanceof AbstractConnectionPool){
            ((AbstractConnectionPool<T>) connectionPool).recycle(this);
        }
    }

    @Override
    public T close() {
        /*
            还没有完成丢弃任务 就 开始丢弃
         */
        if (failureDiscard != null && !failureDiscard.isDone() && !failureDiscard.cancel(false)) {
            log.debug("The discard task was cancelled");
        }
        T temp = getConnection();
        this.connection = null;
        this.connectionPool = null;
        this.failureDiscard = null;
        return temp;
    }
}
