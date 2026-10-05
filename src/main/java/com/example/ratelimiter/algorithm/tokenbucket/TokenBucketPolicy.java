package com.example.ratelimiter.algorithm.tokenbucket;

public final class TokenBucketPolicy {
    private final long capacity;
    private final long refillRatePerSecond;

    public TokenBucketPolicy(long capacity, long refillRatePerSecond) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (refillRatePerSecond <= 0) {
            throw new IllegalArgumentException("refillRatePerSecond must be positive");
        }
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getRefillRatePerSecond() {
        return refillRatePerSecond;
    }
}
