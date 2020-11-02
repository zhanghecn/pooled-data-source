package com.zhanghe.pool.core.collection.state;

/**
 * 集合内元素使用状态
 * @author: ZhangHe
 * @since: 2020/10/22 15:24
 */
public interface UseState {
    //使用
    int USE = 1;

    //未使用
    int UN_USE = 0;

    //删除
    int REMOVE = -1;

    //保留
    int RETAIN = 2;


    int getState();

    void setState(int nowStatus);

    boolean compareSetState(int expect, int update);
}
