package com.zhanghe.pool.core.connection;

import com.zhanghe.pool.core.connection.exception.PoolShutdownException;
import com.zhanghe.pool.core.datasource.IDataSource;

/**
 * 连接池方法
 * @author: ZhangHe
 * @since: 2020/10/20 15:01
 */
public interface ConnectionPool<T> extends IDataSource<T>, AutoCloseable {
    /**
     * 添加连接
     * @return 是否添加成功
     */
    boolean addConnection();


    /**
     * 关闭连接池
     * @throws PoolShutdownException shutdown失败异常
     */
    @Override
    void close() throws PoolShutdownException;


    /**
     * 关闭连接池
     * @throws PoolShutdownException shutdown失败异常
     */
    void shutdown() throws PoolShutdownException;


}



