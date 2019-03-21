package com.dynamic.datasource.util;

import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.dynamic.datasource.ds.DynamicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.sql.DataSource;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//@Component
public class DataSourceRefresher implements ApplicationContextAware {
  private static final Logger logger = LoggerFactory.getLogger(DataSourceRefresher.class);

  private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

  private String changedKey;

  public DataSourceRefresher() {
  }

  public DataSourceRefresher(String changedKey) {
    this.changedKey = changedKey;
  }

  @Autowired
  private DynamicDataSource dynamicDataSource;

  @Autowired
  private DataSourceManager dataSourceManager;

  @Autowired
  private ApplicationContext applicationContext;

  @ApolloConfigChangeListener //"druid-application"
  public void onChange(ConfigChangeEvent changeEvent) {
    boolean dataSourceConfigChanged = false;
    for (String changedKey : changeEvent.changedKeys()) {
      if (changedKey.startsWith(changedKey)) { //"spring.datasource."
        dataSourceConfigChanged = true;
        break;
      }
    }

    Set<String> set = changeEvent.changedKeys();

    if (dataSourceConfigChanged) {
      refreshDataSource(set);
    }
  }

  private synchronized void refreshDataSource(Set<String> changedKeys) {
    try {
      logger.info("Refreshing data source");

      this.applicationContext.publishEvent(new EnvironmentChangeEvent(changedKeys));

      DataSource newDataSource = dataSourceManager.createAndTestDataSource();
      DataSource oldDataSource = dynamicDataSource.setDataSource(newDataSource);
      asyncTerminate(oldDataSource);

      logger.info("Finished refreshing data source");
    } catch (Throwable ex) {
      logger.error("Refreshing data source failed", ex);
    }
  }

  private void asyncTerminate(DataSource dataSource) {
    DataSourceTerminationTask task = new DataSourceTerminationTask(dataSource, scheduledExecutorService);

    //start now
    scheduledExecutorService.schedule(task, 0, TimeUnit.MILLISECONDS);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

//  private String resolverAnno(){
//    Class<Dy> reflectTestClass = APO.class;
//  }
}
