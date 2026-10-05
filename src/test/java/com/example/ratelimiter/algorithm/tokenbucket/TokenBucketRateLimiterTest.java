package com.example.ratelimiter.algorithm.tokenbucket;

import com.example.ratelimiter.RateLimitDecision;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBucketRateLimiterTest {
    @Test
    void startsFullAndRejectsWhenAllTokensAreConsumed() {
        AtomicLong nowMillis = new AtomicLong(1_000);
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(new TokenBucketPolicy(2, 2), nowMillis::get);

        RateLimitDecision first = limiter.check("consumer-a");
        RateLimitDecision second = limiter.check("consumer-a");
        RateLimitDecision rejected = limiter.check("consumer-a");

        assertTrue(first.isAllowed());
        assertEquals(1, first.getRemaining());
        assertTrue(second.isAllowed());
        assertEquals(0, second.getRemaining());
        assertFalse(rejected.isAllowed());
        assertEquals(500, rejected.getRetryAfterMillis());
    }

    @Test
    void refillsOnlyWholeTokensAndReportsTimeUntilNextToken() {
        AtomicLong nowMillis = new AtomicLong(2_000);
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(new TokenBucketPolicy(1, 2), nowMillis::get);
        limiter.check("consumer-a");

        RateLimitDecision rejected = limiter.check("consumer-a");
        nowMillis.addAndGet(250);
        RateLimitDecision stillRejected = limiter.check("consumer-a");
        assertEquals(0, stillRejected.getRemaining());
        assertEquals(250, stillRejected.getRetryAfterMillis());
        nowMillis.addAndGet(250);
        RateLimitDecision allowed = limiter.check("consumer-a");

        assertFalse(rejected.isAllowed());
        assertEquals(500, rejected.getRetryAfterMillis());
        assertFalse(stillRejected.isAllowed());
        assertEquals(250, stillRejected.getRetryAfterMillis());
        assertTrue(allowed.isAllowed());
        assertEquals(0, allowed.getRemaining());
    }

    @Test
    void keepsBucketsIndependentPerConsumerAndCapsRefillAtCapacity() {
        AtomicLong nowMillis = new AtomicLong(3_000);
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(new TokenBucketPolicy(2, 1), nowMillis::get);

        limiter.check("consumer-a");
        limiter.check("consumer-a");
        nowMillis.addAndGet(10_000);

        RateLimitDecision refilled = limiter.check("consumer-a");
        RateLimitDecision independent = limiter.check("consumer-b");

        assertTrue(refilled.isAllowed());
        assertEquals(1, refilled.getRemaining());
        assertTrue(independent.isAllowed());
        assertEquals(1, independent.getRemaining());
    }

    @Test
    void rejectsBlankConsumerIds() {
        TokenBucketRateLimiter limiter =
                new TokenBucketRateLimiter(new TokenBucketPolicy(1, 1));

        assertThrows(IllegalArgumentException.class, () -> limiter.check(" "));
    }
}
