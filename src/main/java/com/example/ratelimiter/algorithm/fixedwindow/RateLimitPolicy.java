package com.example.ratelimiter.algorithm.fixedwindow;

import java.time.Duration;
import java.util.Objects;

public final class RateLimitPolicy {
    private final long requestLimit;
    private final long windowMillis;

    public RateLimitPolicy(long requestLimit, Duration window) {
        Objects.requireNonNull(window, "window");
        if (requestLimit <= 0) {
            throw new IllegalArgumentException("requestLimit must be positive");
        }
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be positive");
        }
        try {
            this.windowMillis = window.toMillis();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("window is too large", exception);
        }
        if (windowMillis <= 0) {
            throw new IllegalArgumentException("window must be at least one millisecond");
        }
        this.requestLimit = requestLimit;
    }

    public long getRequestLimit() {
        return requestLimit;
    }

    public long getWindowMillis() {
        return windowMillis;
    }
}
