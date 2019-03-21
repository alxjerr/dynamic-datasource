package com.dynamic.datasource.util;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DataSourceManager {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceManager.class);

    @Autowired
    private CustomizedConfigurationPropertiesBinder binder;

    @Autowired
    private DataSourceProperties dataSourceProperties;

    /**
     * create a hikari data source
     *
     * @see org.springframework.boot.autoconfigure.jdbc.DataSourceConfiguration.Hikari#dataSource
     */
    public DruidDataSource createDataSource() {
        DruidDataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().type(DruidDataSource.class).build();
        if (StringUtils.hasText(dataSourceProperties.getName())) {
            dataSource.setName(dataSourceProperties.getName());
        }
        Bindable<?> target = Bindable.of(DruidDataSource.class).withExistingValue(dataSource);
        this.binder.bind("spring.datasource.dbcp2", target);
        return dataSource;
    }


    public DruidDataSource createAndTestDataSource() throws SQLException {
        DruidDataSource newDataSource = createDataSource();
        try {
            testConnection(newDataSource);
        } catch (SQLException ex) {
            logger.error("Testing connection for data source failed: {}", newDataSource.getUrl(), ex);
            newDataSource.close();
            throw ex;
        }
        return newDataSource;
    }

    private void testConnection(DruidDataSource dataSource) throws SQLException {
        /*DruidPooledConnection connection = null;
        try {
            connection = dataSource.getConnection();
        }catch (Exception e) {
            //return the connection
        }finally {
            connection.close();
        }*/

        DruidPooledConnection connection = dataSource.getConnection();
        connection.close();
    }
}
