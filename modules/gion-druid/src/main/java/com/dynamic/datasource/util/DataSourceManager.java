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

//@Component
public class DataSourceManager {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceManager.class);

    private String bindTarget;

    public DataSourceManager() {
    }

    public DataSourceManager(String bindTarget) {
        this.bindTarget = bindTarget;
    }

    @Autowired
    private CustomizedConfigurationPropertiesBinder binder;

    @Autowired
    private DataSourceProperties dataSourceProperties;

    public DruidDataSource createDataSource() {
        DruidDataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().type(DruidDataSource.class).build();
        if (StringUtils.hasText(dataSourceProperties.getName())) {
            dataSource.setName(dataSourceProperties.getName());
        }
        Bindable<?> target = Bindable.of(DruidDataSource.class).withExistingValue(dataSource);
//        this.binder.bind("spring.datasource.druid", target);
        this.binder.bind(bindTarget, target);
        return dataSource;
    }//dbcp2


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
        DruidPooledConnection connection = dataSource.getConnection();
        connection.close();
    }
}
