package com.zhanghe.pool.core.connection;

import com.zhanghe.pool.core.collection.state.UseState;

/**
 * 连接池存放的对象
 * @author: ZhangHe
 * @since: 2020/10/21 11:39
 */
public interface PoolObject<T> extends UseState {

    void recycle(long lastAccessed);

    T close();
}
