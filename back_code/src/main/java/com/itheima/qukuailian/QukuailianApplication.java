package com.itheima.qukuailian;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.itheima.qukuailian.mapper")
public class QukuailianApplication {

    public static void main(String[] args) {
        SpringApplication.run(QukuailianApplication.class, args);
    }

}
