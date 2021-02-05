package com.github.zhanghecn.ftp.datasource;

import com.github.zhanghecn.ftp.datasource.exception.FtpConnectionLogoutException;
import com.github.zhanghecn.ftp.datasource.model.FtpProperties;
import com.zhanghe.pool.core.connection.GeneralPool;
import com.zhanghe.pool.core.connection.PoolEntity;
import com.zhanghe.pool.core.connection.factory.ConnectionFactory;
import com.zhanghe.pool.core.util.spring.ConnectionProxyDecorate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.SocketClient;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 简单快捷 快速的连接池
 *
 * @author: ZhangHe
 * @since: 2020/10/29 13:27
 */
@Slf4j
public class SimpleFTPPool extends GeneralPool<FTPClient> {

    //默认获取连接超时时间
    static final long DEFAULT_CONNECTION_TIMEOUT = 10_000;

    //默认验证超时时间
    static final long DEFAULT_VALIDATION_TIMEOUT = 5_000;

    //默认空闲回收超时时间
    static final long DEFAULT_IDLE_TIMEOUT = TimeUnit.MINUTES.toMillis(10);

    //默认最大存活时间
    static final long DEFAULT_MAX_LIFETIME = TimeUnit.MINUTES.toMillis(30);

    //默认最小空闲连接
    static final int DEFAULT_MIN_IDLE = 20;

    static final String DEFAULT_POOL_NAME = "zh-pool";


    public SimpleFTPPool(FTPDataSourceConfig dataSourceConfig) {
        super(dataSourceConfig, new FtpClientConnectionFactory(dataSourceConfig));
    }


    @Override
    protected FTPDataSourceConfig getDataSourceConfig() {
        return (FTPDataSourceConfig) super.getDataSourceConfig();
    }

    @Override
    protected void config() {
        log.debug("{} start", getDataSourceConfig().getPoolName());
    }

    @Override
    protected void init() {
        addConnection();
    }

    @Override
    protected void validate() {
        FTPDataSourceConfig config = getDataSourceConfig();
        if (config.getConnectionTimeout() < DEFAULT_CONNECTION_TIMEOUT) {
            config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        }

        if (config.getValidationTimeout() < DEFAULT_VALIDATION_TIMEOUT) {
            config.setValidationTimeout(DEFAULT_VALIDATION_TIMEOUT);
        }

        if (config.getIdleTimeout() < TimeUnit.SECONDS.toMillis(10)) {
            config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
        }

        if (config.getMaxLifetime() < config.getIdleTimeout()) {
            config.setMaxLifetime(DEFAULT_MAX_LIFETIME);
        }

        if (config.getMinIdle() < 1) {
            config.setMinIdle(DEFAULT_MIN_IDLE);
        }

        if (StringUtils.isEmpty(config.getPoolName())) {
            config.setPoolName(DEFAULT_POOL_NAME);
        }

        if (config.getMaxPoolSize() < config.getMinIdle()) {
            config.setMaxPoolSize(config.getMinIdle());
        }
    }

    @Override
    protected void closeConnection(PoolEntity<FTPClient> conn) {
        try {
            conn.getConnection().logout();
        } catch (IOException e) {
            log.error("错误的登出ftp", e);
            throw new FtpConnectionLogoutException();
        }finally {
            recycle(conn);
        }
    }

    @Override
    protected FTPClient getProxyConnection(PoolEntity<FTPClient> poolEntity) {
        return (FTPClient) ConnectionProxyDecorate.proxyConnection(poolEntity, true, "logout",true);
    }

    @Override
    protected boolean isConnectionAlive(FTPClient con) {
        return con.isConnected();
    }

    @Slf4j
    static class FtpClientConnectionFactory implements ConnectionFactory<FTPClient> {

        FTPDataSourceConfig config;


        public FtpClientConnectionFactory(FTPDataSourceConfig sqlDataSourceConfig) {
            this.config = sqlDataSourceConfig;
        }

        @Override
        public FTPClient getConnection() throws Exception {
            String hostname = config.getHostname();
            int port = config.getPort();
            String username = config.getUsername();
            String password = config.getPassword();
            FtpProperties properties = config.getProperties();

            FTPClient ftpClient = new FTPClient();

            if (!ObjectUtils.isEmpty(properties)) {
                String[] ps = Arrays.stream(BeanUtils.getPropertyDescriptors(SocketClient.class)).map(PropertyDescriptor::getName).toArray(String[]::new);
                BeanUtils.copyProperties(properties, ftpClient, ps);
            }

            ftpClient.connect(hostname, port);
            ftpClient.login(username, password);

            return ftpClient;
        }
    }
}
