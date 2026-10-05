package com.example.ratelimiter.interfaces;

import com.example.ratelimiter.RateLimitDecision;

public interface RateLimiter {
    RateLimitDecision check(String consumerId);
}
