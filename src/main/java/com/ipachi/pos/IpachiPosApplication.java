package com.ipachi.pos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class IpachiPosApplication {
    public static void main(String[] args) {
        SpringApplication.run(IpachiPosApplication.class, args);
    }
}
