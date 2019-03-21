package com.dynamic.datasource.util;

import com.alibaba.druid.pool.DruidDataSource;
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
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class DataSourceRefresher implements ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(DataSourceRefresher.class);

    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    private DynamicDataSource dynamicDataSource;

    @Autowired
    private DataSourceManager dataSourceManager;

    @Autowired
    private ApplicationContext applicationContext;

    private volatile int retryTimes = 0;
    private static final int MAX_RETRY_TIMES = 10;
    private static final int RETRY_DELAY_IN_MILLISECONDS = 5000;

    @ApolloConfigChangeListener(value = {"druid-application"})
    public void onChange(ConfigChangeEvent changeEvent) {
        boolean dataSourceConfigChanged = false;
        for (String changedKey : changeEvent.changedKeys()) {
            if (changedKey.startsWith("spring.datasource.")) {
                dataSourceConfigChanged = true;
                break;
            }
        }

        if (dataSourceConfigChanged) {
            refreshDataSource(changeEvent.changedKeys());
        }
    }

    private synchronized void refreshDataSource(Set<String> changedKeys) {
        try {
            logger.info("Refreshing data source");

            /**
             * rebind configuration beans, e.g. DataSourceProperties
             * @see org.springframework.cloud.context.properties.ConfigurationPropertiesRebinder#onApplicationEvent
             */
            this.applicationContext.publishEvent(new EnvironmentChangeEvent(changedKeys));

            DataSource newDataSource = dataSourceManager.createAndTestDataSource();
            DataSource oldDataSource = dynamicDataSource.setDataSource(newDataSource);
//      asyncTerminate(oldDataSource);
            terminate(oldDataSource);

            logger.info("Finished refreshing data source");
        } catch (Throwable ex) {
            logger.error("Refreshing data source failed", ex);
        }
    }

    private void asyncTerminate(DataSource dataSource) {
//    DataSourceTerminationTask task = new DataSourceTerminationTask(dataSource, scheduledExecutorService);

        //start now
//    scheduledExecutorService.schedule(task, 0, TimeUnit.MILLISECONDS);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private boolean terminate(DataSource dataSource) {
        logger.info("Trying to terminate data source: {}", dataSource);

        try {
            if (dataSource instanceof DruidDataSource) {
                return terminateHikariDataSource((DruidDataSource) dataSource);
            }

            logger.error("Not supported data source: {}", dataSource);

            return true;
        } catch (Throwable ex) {
            logger.warn("Terminating data source {} failed, will retry in {} ms, error message: {}", dataSource,
                    RETRY_DELAY_IN_MILLISECONDS, ex.getMessage());
            return false;
        } finally {
            retryTimes++;
        }
    }


    private boolean terminateHikariDataSource(DruidDataSource dataSource) throws SQLException {
        for (; ; ) {
            dataSource.shrink();

            int activeCount = dataSource.getActiveCount();

            if (activeCount > 0 && retryTimes < MAX_RETRY_TIMES) {
                logger.warn("Data source {} still has {} active connections, will retry in {} ms.", dataSource,
                        activeCount, RETRY_DELAY_IN_MILLISECONDS);
                return false;
            }

            if (activeCount > 0) {
                logger.warn("Retry times({}) >= {}, force closing data source {}, with {} active connections!", retryTimes,
                        MAX_RETRY_TIMES, dataSource, activeCount);
            }

            dataSource.close();

            return true;
        }
    }
}
