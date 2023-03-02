package com.dabing.planabc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.dabing.planabc.mapper")
@SpringBootApplication
public class PlanABCApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlanABCApplication.class,args);
    }
}
