package com.example.ratelimiter.algorithm.leakybucket;

import com.example.ratelimiter.RateLimitDecision;
import com.example.ratelimiter.interfaces.LeakyBucketStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeakyBucketRateLimiterTest {
    @Test
    void mapsAcceptedStoreResultToAllowedDecision() {
        LeakyBucketRateLimiter limiter = limiter(
                new LeakyBucketResult(true, 2, 3, 0));

        RateLimitDecision decision = limiter.check("consumer-a");

        assertTrue(decision.isAllowed());
        assertEquals(5, decision.getLimit());
        assertEquals(3, decision.getRemaining());
        assertEquals(0, decision.getRetryAfterMillis());
    }

    @Test
    void mapsFullBucketStoreResultToRejectedDecision() {
        LeakyBucketRateLimiter limiter = limiter(
                new LeakyBucketResult(false, 5, 0, 1_000));

        RateLimitDecision decision = limiter.check("consumer-a");

        assertFalse(decision.isAllowed());
        assertEquals(5, decision.getLimit());
        assertEquals(0, decision.getRemaining());
        assertEquals(1_000, decision.getRetryAfterMillis());
    }

    @Test
    void sendsConsumerScopedKeyAndConfiguredBucketPolicyToStore() {
        AtomicReference<String> capturedKey = new AtomicReference<>();
        AtomicReference<Long> capturedCapacity = new AtomicReference<>();
        AtomicReference<Long> capturedLeakRatePerSecond = new AtomicReference<>();
        LeakyBucketStore store = (key, capacity, leakRatePerSecond, nowMillis) -> {
            capturedKey.set(key);
            capturedCapacity.set(capacity);
            capturedLeakRatePerSecond.set(leakRatePerSecond);
            return new LeakyBucketResult(true, 1, capacity - 1, 0);
        };
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(
                new LeakyBucketPolicy(7, 5), store);

        limiter.check("client one");

        assertEquals("rate-limit:leaky-bucket:Y2xpZW50IG9uZQ", capturedKey.get());
        assertEquals(7L, capturedCapacity.get());
        assertEquals(5L, capturedLeakRatePerSecond.get());
    }

    @Test
    void rejectsBlankConsumerIdsBeforeCallingStore() {
        AtomicReference<Boolean> storeCalled = new AtomicReference<>(false);
        LeakyBucketStore store = (key, capacity, leakRatePerSecond, nowMillis) -> {
            storeCalled.set(true);
            return new LeakyBucketResult(true, 1, capacity - 1, 0);
        };
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(
                new LeakyBucketPolicy(3, 1), store);

        assertThrows(IllegalArgumentException.class, () -> limiter.check(" "));
        assertFalse(storeCalled.get());
    }

    private static LeakyBucketRateLimiter limiter(LeakyBucketResult result) {
        LeakyBucketStore store = (key, capacity, leakRatePerSecond, nowMillis) -> result;
        return new LeakyBucketRateLimiter(
                new LeakyBucketPolicy(5, 1), store);
    }
}
