package com.example.datasource.test;

import com.github.zhanghecn.ftp.datasource.ZHFTPDataSource;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.SQLException;

/**
 * @author: ZhangHe
 * @since: 2021/2/4 13:20
 */
@SpringBootTest
public class FtpDataSourceTest {
    @Autowired
    ZHFTPDataSource zhftpDataSource;

    @Test
    public void test() throws SQLException {
        FTPClient connectionObject = zhftpDataSource.getConnectionObject();

    }
}
