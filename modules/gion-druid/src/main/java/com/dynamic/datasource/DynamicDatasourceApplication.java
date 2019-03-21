package com.dynamic.datasource;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@EnableApolloConfig
@SpringBootApplication
public class DynamicDatasourceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicDatasourceApplication.class, args);
//        System.out.println("--------------------------------" + ctx.getBean(DataSource.class));
    }

}

