package com.github.zhanghecn.ftp.autoconfig;

import com.github.zhanghecn.ftp.datasource.ZHFTPDataSource;
import com.github.zhanghecn.ftp.datasource.template.FtpTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author: ZhangHe
 * @since: 2021/2/4 11:11
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ZHFTPDataSource.class)
public class FtpDataSourceAutoConfiguration {


    @Bean
    public FtpTemplate ftpTemplate(ZHFTPDataSource dataSource){
        return new FtpTemplate(dataSource);
    }
}
