package com.zhanghe.pool.core.datasource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * SQL连接数据源适配接口
 * @author: ZhangHe
 * @since: 2020/10/20 11:10
 */
public interface DataSourceAdapter extends DataSource,IDataSource<Connection> {

    @Override
   default Connection getConnection(String username, String password) throws SQLException{
        throw new UnsupportedOperationException();
    }
}
