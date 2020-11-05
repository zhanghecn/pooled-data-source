package com.github.zhanghecn.sql.datasource;

import com.zhanghe.pool.core.config.DataSourceConfig;
import lombok.Data;

import java.util.Properties;

/**
 * @author: ZhangHe
 * @since: 2020/10/29 15:27
 */
@Data
public class SQLDataSourceConfig  extends DataSourceConfig {
    private String driverClassName;

    private String username;

    private String password;

    private String jdbcUrl;

    private Properties properties;
}
