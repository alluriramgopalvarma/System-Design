package com.example.ratelimiter.algorithm.fixedwindow;

import com.example.ratelimiter.RateLimitDecision;
import com.example.ratelimiter.interfaces.FixedWindowCounter;
import com.example.ratelimiter.interfaces.RateLimiter;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Base64;
import java.util.Objects;

public final class FixedWindowRateLimiter implements RateLimiter {
    private static final String KEY_PREFIX = "rate-limit:fixed-window:";

    private final RateLimitPolicy policy;
    private final FixedWindowCounter counter;
    private final Clock clock;

    public FixedWindowRateLimiter(RateLimitPolicy policy, FixedWindowCounter counter) {
        this(policy, counter, Clock.systemUTC());
    }

    public FixedWindowRateLimiter(RateLimitPolicy policy, FixedWindowCounter counter, Clock clock) {
        this.policy = Objects.requireNonNull(policy, "policy");
        this.counter = Objects.requireNonNull(counter, "counter");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public RateLimitDecision check(String consumerId) {
        if (consumerId == null || consumerId.trim().isEmpty()) {
            throw new IllegalArgumentException("consumerId must not be empty");
        }

        long nowMillis = clock.millis();
        long windowMillis = policy.getWindowMillis();
        long windowIndex = Math.floorDiv(nowMillis, windowMillis);
        long ttlMillis = windowMillis - Math.floorMod(nowMillis, windowMillis);
        String encodedConsumer = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(consumerId.getBytes(StandardCharsets.UTF_8));
        String key = KEY_PREFIX + encodedConsumer + ":" + windowIndex;

        long count = counter.increment(key, ttlMillis);
        boolean allowed = count <= policy.getRequestLimit();
        long remaining = Math.max(0, policy.getRequestLimit() - count);
        return new RateLimitDecision(allowed, policy.getRequestLimit(), remaining, ttlMillis);
    }
}
