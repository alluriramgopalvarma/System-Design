package com.example.ratelimiter.interfaces;

import com.example.ratelimiter.algorithm.leakybucket.LeakyBucketResult;

public interface LeakyBucketStore {
    /**
     * Atomically leaks elapsed requests, conditionally adds one request, and
     * returns the resulting bucket state for this consumer.
     *
     * @param key unique bucket key, usually scoped to one consumer
     * @param capacity maximum queued requests
     * @param leakRatePerSecond number of queued requests drained per second
     * @param nowMillis current time in milliseconds since the Unix epoch
     */
    LeakyBucketResult tryAcquire(
            String key, long capacity, long leakRatePerSecond, long nowMillis);
}
