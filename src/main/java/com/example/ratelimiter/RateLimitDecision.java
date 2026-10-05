package com.example.ratelimiter;

public final class RateLimitDecision {
    private final boolean allowed;
    private final long limit;
    private final long remaining;
    private final long retryAfterMillis;

    public RateLimitDecision(boolean allowed, long limit, long remaining, long retryAfterMillis) {
        this.allowed = allowed;
        this.limit = limit;
        this.remaining = remaining;
        this.retryAfterMillis = retryAfterMillis;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public long getLimit() {
        return limit;
    }

    public long getRemaining() {
        return remaining;
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }
}
