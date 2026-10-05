package com.example.ratelimiter.algorithm.leakybucket;

public final class LeakyBucketPolicy {
    private final long capacity;
    private final long leakRatePerSecond;

    public LeakyBucketPolicy(long capacity, long leakRatePerSecond) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("bucket capacity must be positive");
        }
        if (leakRatePerSecond <= 0) {
            throw new IllegalArgumentException("leakRatePerSecond must be positive");
        }
        this.capacity = capacity;
        this.leakRatePerSecond = leakRatePerSecond;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getLeakRatePerSecond() {
        return leakRatePerSecond;
    }
}
