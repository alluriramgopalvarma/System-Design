package com.example.ratelimiter;

import com.example.ratelimiter.algorithm.fixedwindow.FixedWindowRateLimiter;
import com.example.ratelimiter.algorithm.fixedwindow.RateLimitPolicy;
import com.example.ratelimiter.algorithm.leakybucket.LeakyBucketRateLimiter;
import com.example.ratelimiter.algorithm.leakybucket.LeakyBucketResult;
import com.example.ratelimiter.factory.RateLimiterFactory;
import com.example.ratelimiter.interfaces.FixedWindowCounter;
import com.example.ratelimiter.interfaces.LeakyBucketStore;
import com.example.ratelimiter.interfaces.RateLimiter;
import com.example.ratelimiter.props.RateLimiterProperties;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {
    @Test
    void allowsUpToLimitForEachConsumerIndependently() {
        RateLimiter limiter = limiter(2, 10_000, new InMemoryCounter(),
                Instant.ofEpochMilli(1_000));

        assertTrue(limiter.check("consumer-a").isAllowed());
        assertTrue(limiter.check("consumer-a").isAllowed());
        assertFalse(limiter.check("consumer-a").isAllowed());
        assertTrue(limiter.check("consumer-b").isAllowed());
    }

    @Test
    void resetsCountAtNextFixedWindow() {
        MutableClock clock = new MutableClock(Instant.ofEpochMilli(9_999));
        RateLimiter limiter = RateLimiterFactory.create(
                RateLimitAlgorithm.FIXED_WINDOW,
                new RateLimitPolicy(1, Duration.ofSeconds(10)),
                new InMemoryCounter(),
                clock);

        assertTrue(limiter.check("consumer-a").isAllowed());
        assertFalse(limiter.check("consumer-a").isAllowed());
        clock.setInstant(Instant.ofEpochMilli(10_000));
        assertTrue(limiter.check("consumer-a").isAllowed());
    }

    @Test
    void reportsRemainingRequestsAndWindowTtl() {
        RateLimiter limiter = limiter(3, 10_000, new InMemoryCounter(),
                Instant.ofEpochMilli(1_000));

        RateLimitDecision decision = limiter.check("consumer-a");

        assertEquals(2, decision.getRemaining());
        assertEquals(3, decision.getLimit());
        assertEquals(9_000, decision.getRetryAfterMillis());
    }

    @Test
    void rejectsBlankConsumerIdsAndUnsupportedAlgorithms() {
        RateLimiter limiter = limiter(1, 10_000, new InMemoryCounter(),
                Instant.ofEpochMilli(1_000));
        assertThrows(IllegalArgumentException.class, () -> limiter.check(" "));
        assertThrows(IllegalArgumentException.class, () -> RateLimitAlgorithm.parse("sliding-window"));
    }

    @Test
    void buildsOneLimiterForEachConfiguredConsumerRule() {
        Map<String, RateLimiterProperties.ConsumerRule> rules = new HashMap<>();
        rules.put("consumer-a", rule(1));
        rules.put("consumer-b", rule(2));
        RateLimiterRegistry registry = new RateLimiterRegistry(
                RateLimitAlgorithm.FIXED_WINDOW, rules, new InMemoryCounter());

        assertTrue(registry.get("consumer-a").check("consumer-a").isAllowed());
        assertFalse(registry.get("consumer-a").check("consumer-a").isAllowed());
        assertTrue(registry.get("consumer-b").check("consumer-b").isAllowed());
        assertFalse(registry.hasConsumer("not-configured"));
    }

    @Test
    void selectsLeakyBucketStrategyFromAlgorithmConfiguration() {
        RateLimiterProperties.ConsumerRule rule = new RateLimiterProperties.ConsumerRule();
        rule.setCapacity(5);
        rule.setLeakRatePerSecond(1);
        LeakyBucketStore store = (key, capacity, leakRatePerSecond, nowMillis) ->
                new LeakyBucketResult(true, 1, capacity - 1, 0);

        RateLimiter limiter = RateLimiterFactory.create(
                RateLimitAlgorithm.parse("LEAKY_BUCKET"),
                rule,
                null,
                store);

        assertTrue(limiter instanceof LeakyBucketRateLimiter);
    }

    private static RateLimiterProperties.ConsumerRule rule(long limit) {
        RateLimiterProperties.ConsumerRule rule = new RateLimiterProperties.ConsumerRule();
        rule.setLimit(limit);
        rule.setWindow(Duration.ofMinutes(1));
        return rule;
    }

    private static RateLimiter limiter(
            long limit, long windowMillis, FixedWindowCounter counter, Instant now) {
        return RateLimiterFactory.create(
                RateLimitAlgorithm.FIXED_WINDOW,
                new RateLimitPolicy(limit, Duration.ofMillis(windowMillis)),
                counter,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    private static final class InMemoryCounter implements FixedWindowCounter {
        private final Map<String, Long> counts = new HashMap<>();

        @Override
        public long increment(String key, long ttlMillis) {
            return counts.merge(key, 1L, Long::sum);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
