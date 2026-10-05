package com.example.ratelimiter.algorithm.leakybucket;

public final class LeakyBucketResult {
    private final boolean allowed;
    private final long currentLevel;
    private final long remainingCapacity;
    private final long retryAfterMillis;

    public LeakyBucketResult(
            boolean allowed, long currentLevel, long remainingCapacity, long retryAfterMillis) {
        this.allowed = allowed;
        this.currentLevel = currentLevel;
        this.remainingCapacity = remainingCapacity;
        this.retryAfterMillis = retryAfterMillis;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getCurrentLevel() {
        return currentLevel;
    }

    public long getRemainingCapacity() {
        return remainingCapacity;
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
