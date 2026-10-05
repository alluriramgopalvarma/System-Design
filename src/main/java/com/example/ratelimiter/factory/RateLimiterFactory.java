package com.example.ratelimiter.factory;

import com.example.ratelimiter.RateLimitAlgorithm;
import com.example.ratelimiter.interfaces.RateLimiter;
import com.example.ratelimiter.interfaces.FixedWindowCounter;
import com.example.ratelimiter.algorithm.fixedwindow.FixedWindowRateLimiter;
import com.example.ratelimiter.algorithm.fixedwindow.RateLimitPolicy;
import com.example.ratelimiter.algorithm.leakybucket.LeakyBucketPolicy;
import com.example.ratelimiter.algorithm.leakybucket.LeakyBucketRateLimiter;
import com.example.ratelimiter.interfaces.LeakyBucketStore;
import com.example.ratelimiter.props.RateLimiterProperties;
import java.time.Clock;
import java.util.Objects;

public final class RateLimiterFactory {
    private RateLimiterFactory() {
    }

    public static RateLimiter create(
            RateLimitAlgorithm algorithm,
            RateLimitPolicy policy,
            FixedWindowCounter counter) {
        return create(algorithm, policy, counter, Clock.systemUTC());
    }

    public static RateLimiter create(
            RateLimitAlgorithm algorithm,
            RateLimitPolicy policy,
            FixedWindowCounter counter,
            Clock clock) {
        Objects.requireNonNull(algorithm, "algorithm");
        switch (algorithm) {
            case FIXED_WINDOW:
                return new FixedWindowRateLimiter(policy, counter, clock);
            case LEAKY_BUCKET:
                throw new IllegalArgumentException(
                        "LEAKY_BUCKET requires a consumer rule and LeakyBucketStore");
            default:
                throw new IllegalArgumentException("Unsupported rate limit algorithm: " + algorithm);
        }
    }

    public static RateLimiter create(
            RateLimitAlgorithm algorithm,
            RateLimiterProperties.ConsumerRule rule,
            FixedWindowCounter fixedWindowCounter,
            LeakyBucketStore leakyBucketStore) {
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(rule, "rule");
        switch (algorithm) {
            case FIXED_WINDOW:
                return new FixedWindowRateLimiter(
                        new RateLimitPolicy(rule.getLimit(), rule.getWindow()),
                        Objects.requireNonNull(fixedWindowCounter, "fixedWindowCounter"));
            case LEAKY_BUCKET:
                return new LeakyBucketRateLimiter(
                        new LeakyBucketPolicy(rule.getCapacity(), rule.getLeakRatePerSecond()),
                        Objects.requireNonNull(leakyBucketStore, "leakyBucketStore"));
            default:
                throw new IllegalArgumentException("Unsupported rate limit algorithm: " + algorithm);
        }
    }
}
