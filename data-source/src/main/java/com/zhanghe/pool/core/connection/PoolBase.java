package com.zhanghe.pool.core.connection;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 设置池的基础状态属性
 * @author: ZhangHe
 * @since: 2020/10/20 15:37
 */
public class PoolBase {

    /**
     * 主池的控制状态，CTL，是一个原子整数包装2个概念领域
     * int 数包含32位数 bit
     * 前 3位代表状态
     * 后29位 代表使用量（不一定非要代表使用量）
     */
    protected final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNABLE,0));


    /**
     * 数量位数
     */
    static final int COUNT_BIT = 32 - 3;


    static final int STATE_NUM = -1 << COUNT_BIT;

    static final int MAX_COUNT = ~STATE_NUM;

    /**
     * 运行状态
     */
    static final int RUNNABLE = 0;

    static final int STOP = 2 << COUNT_BIT;
    /**
     * 关闭状态
     */
    static final int SHUTDOWN = STATE_NUM;

    static int ctlOf(int s,int c){
        return s|c;
    }

    static int state(int ctl){
        return ctl & STATE_NUM ;
    }

    static int count(int ctl){
        return ctl &  MAX_COUNT;
    }

    protected boolean isRunnable(){
        return state(ctl.get()) == RUNNABLE;
    }

    protected int size(){
        return count(ctl.get());
    }

    protected boolean compareAndIncrementCount(int expect){
      return  ctl.compareAndSet(expect,expect + 1);
    }

    protected boolean compareAndDecrementCount(int expect){
        return ctl.compareAndSet(expect,expect - 1);
    }

    protected void incrementCount(){
        do{}while (!compareAndIncrementCount(ctl.get()));
    }

    protected void decrementCount(){
        do{}while (!compareAndDecrementCount(ctl.get()));
    }

    protected int status(){
        return PoolBase.state(ctl.get());
    }

}
