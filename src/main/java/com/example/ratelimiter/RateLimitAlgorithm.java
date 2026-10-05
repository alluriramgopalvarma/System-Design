package com.example.ratelimiter;

import java.util.Locale;

public enum RateLimitAlgorithm {
    FIXED_WINDOW,
    LEAKY_BUCKET;

    public static RateLimitAlgorithm parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Rate limit algorithm must not be empty");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported rate limit algorithm: " + value, exception);
        }
    }
}
