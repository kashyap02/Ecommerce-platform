package com.ecommerce.ratelimiterservice.algorithm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlidingWindowRateLimiter {

    private final StringRedisTemplate redisTemplate;

    /*
     * Sliding Window Algorithm — Lua script for atomicity.
     *
     * KEYS[1] = sorted set key   (e.g. "sw:user@test.com")
     * ARGV[1] = window size      (60 seconds)
     * ARGV[2] = max requests     (10 per window)
     * ARGV[3] = current time     (epoch milliseconds)
     *
     * Logic:
     * 1. Remove timestamps older than (now - window)
     * 2. Count remaining entries
     * 3. If count < limit → add current timestamp, allow
     * 4. Else → reject
     */
    private static final String SLIDING_WINDOW_SCRIPT = """
            local key = KEYS[1]
            local window = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local window_start = now - (window * 1000)
                        
            redis.call('ZREMRANGEBYSCORE', key, 0, window_start)
                        
            local count = redis.call('ZCARD', key)
                        
            if count < limit then
                redis.call('ZADD', key, now, now)
                redis.call('EXPIRE', key, window)
                return {1, limit - count - 1}
            else
                return {0, 0}
            end
            """;

    private static final int WINDOW_SIZE = 60;   // seconds
    private static final int MAX_REQUESTS = 10;   // per window

    public boolean isAllowed(String clientId) {
        String key = "sw:" + clientId;
        long now = System.currentTimeMillis();

        RedisScript<List> script = RedisScript.of(SLIDING_WINDOW_SCRIPT, List.class);

        List<Long> result = redisTemplate.execute(
                script,
                List.of(key),
                String.valueOf(WINDOW_SIZE),
                String.valueOf(MAX_REQUESTS),
                String.valueOf(now)
        );

        boolean allowed = result != null && result.get(0) == 1L;
        long remaining = result != null ? result.get(1) : 0;

        log.info("SlidingWindow — clientId={}, allowed={}, remaining={}",
                clientId, allowed, remaining);

        return allowed;
    }
}