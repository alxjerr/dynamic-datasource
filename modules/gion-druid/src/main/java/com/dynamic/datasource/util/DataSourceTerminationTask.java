package com.dynamic.datasource.util;

import com.alibaba.druid.pool.DruidDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataSourceTerminationTask implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(DataSourceTerminationTask.class);
  private static final int MAX_RETRY_TIMES = 10;
  private static final int RETRY_DELAY_IN_MILLISECONDS = 5000;

  private final DataSource dataSourceToTerminate;
  private final ScheduledExecutorService scheduledExecutorService;

  private volatile int retryTimes;

  public DataSourceTerminationTask(DataSource dataSourceToTerminate,
      ScheduledExecutorService scheduledExecutorService) {
    this.dataSourceToTerminate = dataSourceToTerminate;
    this.scheduledExecutorService = scheduledExecutorService;
    this.retryTimes = 0;
  }

  @Override
  public void run() {
    if (terminate(dataSourceToTerminate)) {
      logger.info("Data source {} terminated successfully!", dataSourceToTerminate);
    } else {
      scheduledExecutorService.schedule(this, RETRY_DELAY_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
    }
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

  /**
   * @see <a href="https://github.com/brettwooldridge/HikariCP/issues/742">Support graceful shutdown of connection
   * pool</a>
   */
  private boolean terminateHikariDataSource(DruidDataSource dataSource) throws SQLException {

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
