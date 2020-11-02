package com.zhanghe.pool.core.connection.recycle;

/**
 * 回收策略
 * @author: ZhangHe
 * @since: 2020/10/21 15:59
 */
public interface RecycleStrategy<T> {
    boolean recycle(T t);
}
