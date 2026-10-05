package com.example.ratelimiter.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.ratelimiter")
public class RateLimiterDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(RateLimiterDemoApplication.class, args);
    }
}
