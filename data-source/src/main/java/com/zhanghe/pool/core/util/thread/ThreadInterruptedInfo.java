package com.zhanghe.pool.core.util.thread;

/**
 * 线程中断异常信息
 * @author: ZhangHe
 * @since: 2020/10/21 15:17
 */
public class ThreadInterruptedInfo extends RuntimeException {
    public ThreadInterruptedInfo(String message) {
        super(message);
    }
}
