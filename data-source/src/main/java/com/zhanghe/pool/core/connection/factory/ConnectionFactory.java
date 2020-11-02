package com.zhanghe.pool.core.connection.factory;

/**
 * @author: ZhangHe
 * @since: 2020/10/27 16:24
 */
public interface ConnectionFactory<T> {
    T getConnection() throws Exception;
}
