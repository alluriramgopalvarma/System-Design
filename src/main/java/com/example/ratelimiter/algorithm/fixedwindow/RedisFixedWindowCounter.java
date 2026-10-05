package com.example.ratelimiter.algorithm.fixedwindow;

import com.example.ratelimiter.interfaces.FixedWindowCounter;
import redis.clients.jedis.JedisPooled;

import java.util.Collections;

public final class RedisFixedWindowCounter implements FixedWindowCounter {
    private static final String INCREMENT_SCRIPT =
            "local count = redis.call('INCR', KEYS[1]) " +
            "if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end " +
            "return count";

    private final JedisPooled redis;

    public RedisFixedWindowCounter(JedisPooled redis) {
        this.redis = redis;
    }

    @Override
    public long increment(String key, long ttlMillis) {
        Object response = redis.eval(
                INCREMENT_SCRIPT,
                Collections.singletonList(key),
                Collections.singletonList(Long.toString(ttlMillis)));
        return ((Number) response).longValue();
    }
}
