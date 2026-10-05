package com.example.ratelimiter.interfaces;

public interface FixedWindowCounter {
    long increment(String key, long ttlMillis);
}
