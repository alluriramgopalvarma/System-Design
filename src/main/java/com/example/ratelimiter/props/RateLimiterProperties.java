package com.example.ratelimiter.props;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {
    private String algorithm = "FIXED_WINDOW";
    private String redisUrl = "redis://localhost:6379";
    private String consumerIdHeader = "X-Consumer-Id";
    private Map<String, ConsumerRule> consumers = new LinkedHashMap<>();

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getRedisUrl() {
        return redisUrl;
    }

    public void setRedisUrl(String redisUrl) {
        this.redisUrl = redisUrl;
    }

    public String getConsumerIdHeader() {
        return consumerIdHeader;
    }

    public void setConsumerIdHeader(String consumerIdHeader) {
        this.consumerIdHeader = consumerIdHeader;
    }

    public Map<String, ConsumerRule> getConsumers() {
        return consumers;
    }

    public void setConsumers(Map<String, ConsumerRule> consumers) {
        this.consumers = consumers;
    }

    public static class ConsumerRule {
        private long limit;
        private Duration window = Duration.ofMinutes(1);
        // Leaky-bucket settings: maximum queued requests and drain rate in requests per second.
        private long capacity;
        private long leakRatePerSecond;

        public long getLimit() {
            return limit;
        }

        public void setLimit(long limit) {
            this.limit = limit;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }

        public long getLeakRatePerSecond() {
            return leakRatePerSecond;
        }

        public void setLeakRatePerSecond(long leakRatePerSecond) {
            this.leakRatePerSecond = leakRatePerSecond;
        }
    }
}
