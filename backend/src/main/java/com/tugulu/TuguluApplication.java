package com.tugulu;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.tugulu.mapper")
@EnableScheduling
public class TuguluApplication {

    public static void main(String[] args) {
        SpringApplication.run(TuguluApplication.class, args);
    }
}
