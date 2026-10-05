package com.example.ratelimiter.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;

public final class LocalRunner {
    private LocalRunner() {
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(RateLimiterDemoApplication.class);
        Map<String, Object> localDefaults = new LinkedHashMap<>();
        localDefaults.put("server.port", 8080);
        localDefaults.put("rate-limiter.algorithm", "FIXED_WINDOW");
        localDefaults.put("rate-limiter.redis-url", "${REDIS_URL:redis://localhost:6379}");
        localDefaults.put("rate-limiter.consumer-id-header", "X-Consumer-Id");
        localDefaults.put("rate-limiter.consumers.demo-client.limit", 3);
        localDefaults.put("rate-limiter.consumers.demo-client.window", "1m");
        localDefaults.put("rate-limiter.consumers.partner-client.limit", 10);
        localDefaults.put("rate-limiter.consumers.partner-client.window", "1m");
        application.setDefaultProperties(localDefaults);
        application.run(args);
    }
}
