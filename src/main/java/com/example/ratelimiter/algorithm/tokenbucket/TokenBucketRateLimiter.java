package com.example.ratelimiter.algorithm.tokenbucket;

import com.example.ratelimiter.RateLimitDecision;
import com.example.ratelimiter.interfaces.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;

public final class TokenBucketRateLimiter implements RateLimiter {
    private final TokenBucketPolicy policy;
    private final ConcurrentMap<String, BucketState> buckets = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public TokenBucketRateLimiter(TokenBucketPolicy policy) {
        this(policy, System::currentTimeMillis);
    }

    TokenBucketRateLimiter(TokenBucketPolicy policy, LongSupplier clock) {
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        if (clock == null) {
            throw new IllegalArgumentException("clock must not be null");
        }
        this.policy = policy;
        this.clock = clock;
    }

    @Override
    public RateLimitDecision check(String consumerId) {
        if (consumerId == null || consumerId.trim().isEmpty()) {
            throw new IllegalArgumentException("consumerId must not be empty");
        }

        long nowMillis = clock.getAsLong();
        BucketState bucket = buckets.computeIfAbsent(
                consumerId,
                id -> new BucketState(policy.getCapacity(), nowMillis));

        synchronized (bucket) {
            long elapsedMillis = nowMillis - bucket.lastRefillMillis;
            if (elapsedMillis > 0) {
                if (bucket.tokens < policy.getCapacity()) {
                    double refillCredit = elapsedMillis
                            * (policy.getRefillRatePerSecond() / 1_000.0)
                            + bucket.refillRemainder;
                    long wholeTokensToAdd = (long) Math.floor(refillCredit);
                    if (wholeTokensToAdd >= policy.getCapacity() - bucket.tokens) {
                        bucket.tokens = policy.getCapacity();
                        bucket.refillRemainder = 0;
                    } else {
                        bucket.tokens += wholeTokensToAdd;
                        bucket.refillRemainder = refillCredit - wholeTokensToAdd;
                    }
                } else {
                    bucket.refillRemainder = 0;
                }
                bucket.lastRefillMillis = nowMillis;
            }

            boolean isAllowed = bucket.tokens > 0;
            long retryAfterMillis = 0;
            if (isAllowed) {
                bucket.tokens -= 1;
            } else {
                double millisUntilToken =
                        (1 - bucket.refillRemainder) * 1_000.0
                                / policy.getRefillRatePerSecond();
                retryAfterMillis = (long) Math.ceil(millisUntilToken);
            }

            return new RateLimitDecision(
                    isAllowed,
                    policy.getCapacity(),
                    (long) bucket.tokens,
                    retryAfterMillis);
        }
    }

    private static final class BucketState {
        private long tokens;
        private double refillRemainder;
        private long lastRefillMillis;

        private BucketState(long initialTokens, long lastRefillMillis) {
            this.tokens = initialTokens;
            this.lastRefillMillis = lastRefillMillis;
        }
    }
}
