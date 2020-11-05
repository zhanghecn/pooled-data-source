package com.github.zhanghecn.sql.datasource;

import com.zhanghe.pool.core.connection.GeneralPool;
import com.zhanghe.pool.core.connection.PoolEntity;
import com.zhanghe.pool.core.connection.factory.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * 简单快捷 快速的连接池
 *
 * @author: ZhangHe
 * @since: 2020/10/29 13:27
 */
@Slf4j
public class SimpleSqlPool extends GeneralPool<Connection> {

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


    public SimpleSqlPool(SQLDataSourceConfig dataSourceConfig) {
        super(dataSourceConfig, new SQLConnectionFactory(dataSourceConfig));
    }


    @Override
    protected SQLDataSourceConfig getDataSourceConfig() {
        return (SQLDataSourceConfig) super.getDataSourceConfig();
    }

    @Override
    protected void config() {
        log.debug("{} start",getDataSourceConfig().getPoolName());
    }

    @Override
    protected void init() {
        addConnection();
    }

    @Override
    protected void validate() {
        SQLDataSourceConfig config = getDataSourceConfig();
        if(config.getConnectionTimeout() < DEFAULT_CONNECTION_TIMEOUT){
            config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        }

        if(config.getValidationTimeout() < DEFAULT_VALIDATION_TIMEOUT){
            config.setValidationTimeout(DEFAULT_VALIDATION_TIMEOUT);
        }

        if(config.getIdleTimeout() < TimeUnit.SECONDS.toMillis(10)){
            config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
        }

        if(config.getMaxLifetime() < config.getIdleTimeout()){
            config.setMaxLifetime(DEFAULT_MAX_LIFETIME);
        }

        if(config.getMinIdle() < 1){
            config.setMinIdle(DEFAULT_MIN_IDLE);
        }

        if(StringUtils.isEmpty(config.getPoolName())){
            config.setPoolName(DEFAULT_POOL_NAME);
        }

        if(config.getMaxPoolSize() < config.getMinIdle()){
            config.setMaxPoolSize(config.getMinIdle());
        }
    }



    @Override
    protected Connection getProxyConnection(PoolEntity<Connection> poolEntity) {
        return ConnectionProxyDecorate.proxyConnection(poolEntity);
    }

    @Override
    protected boolean isConnectionAlive(Connection con) {
        try {
            return con.isValid((int) (getDataSourceConfig().getConnectionTimeout()/1000));
        } catch (SQLException e) {
            log.warn("failed alive",e);
            return false;
        }
    }

    @Slf4j
    static class SQLConnectionFactory implements ConnectionFactory<Connection> {

        private static final String PASSWORD = "password";
        private static final String USER = "user";

        SQLDataSourceConfig config;

        Driver driver;

        Properties userInfo;


        public SQLConnectionFactory(SQLDataSourceConfig sqlDataSourceConfig) {
            this.config = sqlDataSourceConfig;
            loadProperty();
            loadDriver();
        }

        void loadProperty() {
            this.userInfo = new Properties();

            Properties other;
            if((other = config.getProperties()) != null){
                this.userInfo.putAll(other);
            }

            this.userInfo.setProperty(USER, config.getUsername());
            this.userInfo.setProperty(PASSWORD, config.getPassword());

        }

        void loadDriver() {

            String driverClassName = config.getDriverClassName();
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                if (driver.getClass().getName().equals(driverClassName)) {
                    this.driver = driver;
                }
            }

            if (driver == null) {
                Class driverClass = Optional
                        .ofNullable(loadDriverClass(Thread.currentThread().getContextClassLoader(), driverClassName, "current thread"))
                        .orElse(loadDriverClass(this.getClass().getClassLoader(), driverClassName, "current config "));

                try {
                    driver = (Driver) driverClass.newInstance();
                } catch (Exception e) {
                    log.warn("install driver class failed :{}", driverClassName, e);
                }

                try {
                    if (driver == null) {
                        driver = DriverManager.getDriver(config.getJdbcUrl());
                    } else if (!driver.acceptsURL(config.getJdbcUrl())) {
                        throw new RuntimeException("驱动类无法使用JDBC连接:" + config.getJdbcUrl());
                    }

                } catch (SQLException e) {
                    throw new RuntimeException("无法通过jdbc连接实例化Driver" + config.getJdbcUrl());
                }

            }
        }

        Class loadDriverClass(ClassLoader classLoader, String driverClassName, String msg) {
            return Optional.ofNullable(classLoader).map(loader -> {
                try {
                    return loader.loadClass(driverClassName);
                } catch (ClassNotFoundException e) {
                    log.debug("{} class Loader find driver {} fail", msg, driverClassName);
                }
                return null;
            }).orElse(null);
        }

        @Override
        public Connection getConnection() throws Exception {
            return driver.connect(config.getJdbcUrl(),this.userInfo);
        }
    }
}
