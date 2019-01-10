package com.dynamic.datasource;

import com.dynamic.datasource.ds.DynamicDataSource;
import com.dynamic.datasource.util.DataSourceManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class RefreshableDataSourceConfiguration {

  @Bean
  public DynamicDataSource dataSource(DataSourceManager dataSourceManager) {
    DataSource actualDataSource = dataSourceManager.createDataSource();
    return new DynamicDataSource(actualDataSource);
  }
}
