package com.zhanghe.pool.core.util.lock;

import java.util.concurrent.Semaphore;

/**
 * 锁适配器
 * @author: ZhangHe
 * @since: 2020/10/23 16:07
 */
public class EmptySemaphore extends Semaphore {

    public EmptySemaphore() {
        super(0);
    }

    @Override
    public void acquire() throws InterruptedException {
    }

    @Override
    public void acquireUninterruptibly() {
    }

    @Override
    public void release(int permits) {
    }

}
