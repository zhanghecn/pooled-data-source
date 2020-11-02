package com.zhanghe.pool.core.datasource;

/**
 * 数据源
 * @author: ZhangHe
 * @since: 2020/10/20 11:08
 */
public interface IDataSource<T> {
    /**
     * 获取连接
     * @return 连接
     */
    T getConnectionObject() throws Exception;
}
