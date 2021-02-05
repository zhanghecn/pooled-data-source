package com.github.zhanghecn.ftp.datasource;

import com.zhanghe.pool.core.connection.ConnectionPool;
import com.zhanghe.pool.core.datasource.IDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.sql.SQLException;

/**
 * zh 构建连接池示例模板
 *
 * @author: ZhangHe
 * @since: 2020/10/31 13:50
 */
@Slf4j
@ConfigurationProperties("spring.ftp")
public class ZHFTPDataSource extends FTPDataSourceConfig implements IDataSource<FTPClient>, AutoCloseable {
    private ConnectionPool connectionPool;

    @Override
    public FTPClient getConnectionObject() throws SQLException {
        try {
            if (connectionPool == null) {
                synchronized (this) {
                    if (connectionPool == null) {
                        createDefaultConnectionPool();
                    }
                }
            }
            return (FTPClient) connectionPool.getConnectionObject();
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            log.error("额外的获取连接异常", e);
            return null;
        }
    }

    private void createDefaultConnectionPool() {
        this.connectionPool = new SimpleFTPPool(this);
    }

    @Override
    public void close() throws Exception {
        connectionPool.close();
    }
}
