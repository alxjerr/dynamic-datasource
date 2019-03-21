//package com.dynamic.datasource.util;
//
//import com.alibaba.druid.pool.DruidDataSource;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.sql.DataSource;
//import java.sql.SQLException;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//public class DataSourceTerminationTask implements Runnable {
//
//  private static final Logger logger = LoggerFactory.getLogger(DataSourceTerminationTask.class);
//  private static final int MAX_RETRY_TIMES = 10;
//  private static final int RETRY_DELAY_IN_MILLISECONDS = 5000;
//
//  private final DataSource dataSourceToTerminate;
//  private final ScheduledExecutorService scheduledExecutorService;
//
//  private volatile int retryTimes;
//
//  public DataSourceTerminationTask(DataSource dataSourceToTerminate,
//      ScheduledExecutorService scheduledExecutorService) {
//    this.dataSourceToTerminate = dataSourceToTerminate;
//    this.scheduledExecutorService = scheduledExecutorService;
//    this.retryTimes = 0;
//  }
//
//  @Override
//  public void run() {
//    if (terminate(dataSourceToTerminate)) {
//      logger.info("Data source {} terminated successfully!", dataSourceToTerminate);
//    } else {
//      scheduledExecutorService.schedule(this, RETRY_DELAY_IN_MILLISECONDS, TimeUnit.MILLISECONDS);
//    }
//  }
//
//
//}
