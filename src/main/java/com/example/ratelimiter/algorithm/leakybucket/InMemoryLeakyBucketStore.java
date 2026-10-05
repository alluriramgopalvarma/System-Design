package com.example.ratelimiter.algorithm.leakybucket;

import com.example.ratelimiter.interfaces.LeakyBucketStore;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryLeakyBucketStore implements LeakyBucketStore {
    private final ConcurrentMap<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

    @Override
    public LeakyBucketResult tryAcquire(
            String key, long capacity, long leakRatePerSecond, long nowMillis) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key must not be empty");
        }
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        if (leakRatePerSecond <= 0) {
            throw new IllegalArgumentException("leakRatePerSecond must be positive");
        }

        Deque<Long> arrivals = buckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (arrivals) {
            long rateOfConsumptionMillis =
                    1_000 / leakRatePerSecond
                            + (1_000 % leakRatePerSecond == 0 ? 0 : 1);
            rateOfConsumptionMillis = Math.max(1, rateOfConsumptionMillis);

            while (!arrivals.isEmpty()
                    && nowMillis - arrivals.peekFirst() >= rateOfConsumptionMillis) {
                arrivals.pollFirst();
            }

            boolean isAllowed = arrivals.size() < capacity;
            if (isAllowed) {
                arrivals.addLast(nowMillis);
            }

            long retryAfterMillis = isAllowed
                    ? 0
                    : Math.max(
                            0,
                            rateOfConsumptionMillis - (nowMillis - arrivals.peekFirst()));

            return new LeakyBucketResult(
                    isAllowed,
                    arrivals.size(),
                    capacity - arrivals.size(),
                    retryAfterMillis);
        }
    }
}
