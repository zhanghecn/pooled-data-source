package com.github.zhanghecn.sql.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;

/**
 * 自动装配
 * @author: ZhangHe
 * @since: 2020/11/2 14:44
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ DataSource.class})
@EnableConfigurationProperties(DataSourceProperties.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class ZHDataSourceAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean({ DataSource.class})
    @Import({ ZHDataSourceConfiguration.class })
    protected static class PooledDataSourceConfiguration {
    }
}
