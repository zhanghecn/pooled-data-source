package com.zhanghe.pool.core.connection.exception;

/**
 * @author: ZhangHe
 * @since: 2020/10/27 17:11
 */
public class PoolConnectionDepleteException extends RuntimeException {
    public PoolConnectionDepleteException() {
        super("连接池被耗尽");
    }
}
