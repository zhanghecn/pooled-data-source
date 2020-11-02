package com.github.zhanghecn.sql.datasource;

import com.zhanghe.pool.core.connection.ConnectionPool;
import com.zhanghe.pool.core.datasource.DataSourceAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * zh 构建连接池示例模板
 *
 * @author: ZhangHe
 * @since: 2020/10/31 13:50
 */
@Slf4j
@ConfigurationProperties("spring.datasource.zh")
public class ZHDataSource extends SQLDataSourceConfig implements DataSourceAdapter, AutoCloseable {
    private ConnectionPool connectionPool;

    @Override
    public Connection getConnectionObject() throws SQLException {
        try {
            synchronized (this) {
                if (connectionPool == null) {
                    createDefaultConnectionPool();
                }
            }
            return (Connection) connectionPool.getConnectionObject();
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            log.error("额外的获取连接异常", e);
            return null;
        }
    }

    private void createDefaultConnectionPool() {
        this.connectionPool = new SimpleSqlPool(this);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getConnectionObject();
    }


    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        DriverManager.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public void close() throws Exception {
        connectionPool.close();
    }
}
