package com.github.zhanghecn.sql.autoconfig;

import com.github.zhanghecn.sql.datasource.ZHDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * zhDatasource自动配置
 * @author: ZhangHe
 * @since: 2020/11/2 14:19
 */
@Configuration
@ConditionalOnMissingBean(DataSource.class)
public class ZHDataSourceConfiguration {

    /**
     * 当存在 spring.datasource.type 并且设置了 ZHDataSource 数据源类型
     * @param dataSourceProperties 获取datasource配置
     * @return 返回 ZHDataSource 数据源
     */
    @Bean
    @ConditionalOnProperty(name = "spring.datasource.type", havingValue = "com.github.zhanghecn.sql.datasource.ZHDataSource",
            matchIfMissing = true)
    ZHDataSource dataSource(DataSourceProperties dataSourceProperties){
        ZHDataSource zhDataSource = dataSourceProperties
                .initializeDataSourceBuilder().type(ZHDataSource.class)
                .build();
        return zhDataSource;
    }
}
