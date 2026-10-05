package com.example.ratelimiter.algorithm.leakybucket;

import com.example.ratelimiter.RateLimitDecision;
import com.example.ratelimiter.interfaces.LeakyBucketStore;
import com.example.ratelimiter.interfaces.RateLimiter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public final class LeakyBucketRateLimiter implements RateLimiter {
    private static final String KEY_PREFIX = "rate-limit:leaky-bucket:";

    private final LeakyBucketPolicy policy;
    private final LeakyBucketStore store;

    public LeakyBucketRateLimiter(LeakyBucketPolicy policy, LeakyBucketStore store) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.store = Objects.requireNonNull(store, "store");
    }

    @Override
    public RateLimitDecision check(String consumerId) {
        if (consumerId == null || consumerId.trim().isEmpty()) {
            throw new IllegalArgumentException("consumerId must not be empty");
        }

        String encodedConsumer = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(consumerId.getBytes(StandardCharsets.UTF_8));
        String key = KEY_PREFIX + encodedConsumer;

        LeakyBucketResult result = store.tryAcquire(
                key,
                policy.getCapacity(),
                policy.getLeakRatePerSecond(),
                System.currentTimeMillis());
        return new RateLimitDecision(
                result.isAllowed(),
                policy.getCapacity(),
                result.getRemainingCapacity(),
                result.getRetryAfterMillis());
    }
}
