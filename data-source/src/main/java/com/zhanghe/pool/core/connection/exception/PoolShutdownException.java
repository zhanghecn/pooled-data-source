package com.zhanghe.pool.core.connection.exception;

/**
 * 连接池关闭异常
 * @author: ZhangHe
 * @since: 2020/10/20 15:20
 */
public class PoolShutdownException extends Exception {
    public PoolShutdownException() {
        super();
    }

    public PoolShutdownException(String message) {
        super(message);
    }

    public PoolShutdownException(String message, Throwable cause) {
        super(message, cause);
    }

    public PoolShutdownException(Throwable cause) {
        super(cause);
    }

    protected PoolShutdownException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
