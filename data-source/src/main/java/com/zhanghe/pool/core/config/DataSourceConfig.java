package com.zhanghe.pool.core.config;

import lombok.Data;

/**
 * 基本数据源配置类
 * @author: ZhangHe
 * @since: 2020/10/20 11:11
 */
@Data
public abstract class DataSourceConfig {
    /**
     * 获取连接超时时间
     */
    private long connectionTimeout;

    /**
     * 验证连接超时时间
     */
    private long validationTimeout;

    /**
     * 空闲连接超时时间
     */
    private long idleTimeout;

    /**
     * 最大存活时间
     */
    private long maxLifetime;

    /**
     * 最大连接池数量
     */
    private int maxPoolSize;

    /**
     * 最小空闲数量
     */
    private int minIdle;

    /**
     * 连接池名称
     */
    private String poolName;

}
