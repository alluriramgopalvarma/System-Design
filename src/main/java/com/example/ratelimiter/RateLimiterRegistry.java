package com.example.ratelimiter;

import com.example.ratelimiter.interfaces.RateLimiter;
import com.example.ratelimiter.factory.RateLimiterFactory;
import com.example.ratelimiter.interfaces.FixedWindowCounter;
import com.example.ratelimiter.interfaces.LeakyBucketStore;
import com.example.ratelimiter.props.RateLimiterProperties;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RateLimiterRegistry {
    private final Map<String, RateLimiter> limiters;

    public RateLimiterRegistry(
            RateLimitAlgorithm algorithm,
            Map<String, RateLimiterProperties.ConsumerRule> rules,
            FixedWindowCounter counter) {
        this(algorithm, rules, counter, null);
    }

    public RateLimiterRegistry(
            RateLimitAlgorithm algorithm,
            Map<String, RateLimiterProperties.ConsumerRule> rules,
            FixedWindowCounter fixedWindowCounter,
            LeakyBucketStore leakyBucketStore) {
        Objects.requireNonNull(rules, "rules");
        Map<String, RateLimiter> configuredLimiters = new LinkedHashMap<>();
        rules.forEach((consumerId, rule) -> {
            if (consumerId == null || consumerId.trim().isEmpty()) {
                throw new IllegalArgumentException("Configured consumer ID must not be empty");
            }
            if (rule == null) {
                throw new IllegalArgumentException("Rule must be configured for consumer: " + consumerId);
            }
            configuredLimiters.put(consumerId, RateLimiterFactory.create(
                    algorithm, rule, fixedWindowCounter, leakyBucketStore));
        });
        this.limiters = Collections.unmodifiableMap(configuredLimiters);
    }

    public RateLimiter get(String consumerId) {
        return limiters.get(consumerId);
    }

    public boolean hasConsumer(String consumerId) {
        return limiters.containsKey(consumerId);
    }
}
