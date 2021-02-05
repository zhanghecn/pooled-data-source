package com.github.zhanghecn.ftp.datasource.template;

import com.github.zhanghecn.ftp.datasource.ZHFTPDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;

/**
 * @author: ZhangHe
 * @since: 2020/11/3 13:59
 */
@Slf4j
public class FtpTemplate implements ITemplate<FTPClient> {
    ZHFTPDataSource dataSource;

    public FtpTemplate(ZHFTPDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public FTPClient getClient() {
        try {
            return dataSource.getConnectionObject();
        } catch (Exception e) {
            log.error("无法获取ftp连接", e);
        }
        return null;
    }
}
