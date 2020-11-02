package com.zhanghe.pool.core.collection.state;

/**
 * 状态处理
 * 比如我添加元素
 * 也算作一种状态上的改变
 * @author: ZhangHe
 * @since: 2020/10/22 17:23
 */
public interface StateHandler {

    /**
     * 执行某种要求，已达到状态的改变
     * @param waiterCount 大约有多少个线程需要这种状态
     */
    void execute(int waiterCount);
}
