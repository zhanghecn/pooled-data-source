package com.github.zhanghecn.ftp.datasource;

import com.github.zhanghecn.ftp.datasource.model.FtpProperties;
import com.zhanghe.pool.core.config.DataSourceConfig;
import lombok.Data;

/**
 * @author: ZhangHe
 * @since: 2020/10/29 15:27
 */
@Data
public class FTPDataSourceConfig extends DataSourceConfig {
    private String hostname = "localhost";

    private int port = 21;

    private String username;

    private String password;

    private FtpProperties properties;
}
