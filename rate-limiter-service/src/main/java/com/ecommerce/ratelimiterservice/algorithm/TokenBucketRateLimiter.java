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
public class TokenBucketRateLimiter {

    private final StringRedisTemplate redisTemplate;

    /*
     * Token Bucket Algorithm — Lua script for atomicity.
     *
     * KEYS[1] = token count key  (e.g. "tb:user@test.com:tokens")
     * KEYS[2] = last refill key  (e.g. "tb:user@test.com:last_refill")
     * ARGV[1] = max capacity     (10 tokens)
     * ARGV[2] = refill rate      (1 token per second)
     * ARGV[3] = current time     (epoch seconds)
     *
     * Logic:
     * 1. Calculate how many tokens to add based on elapsed time
     * 2. Add tokens (up to max capacity)
     * 3. If tokens > 0, consume 1 and allow
     * 4. Else reject
     */
    private static final String TOKEN_BUCKET_SCRIPT = """
            local tokens_key = KEYS[1]
            local last_refill_key = KEYS[2]
            local capacity = tonumber(ARGV[1])
            local refill_rate = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
                        
            local last_refill = tonumber(redis.call('GET', last_refill_key) or now)
            local current_tokens = tonumber(redis.call('GET', tokens_key) or capacity)
                        
            local elapsed = math.max(0, now - last_refill)
            local new_tokens = math.min(capacity, current_tokens + (elapsed * refill_rate))
                        
            if new_tokens >= 1 then
                redis.call('SET', tokens_key, new_tokens - 1, 'EX', 3600)
                redis.call('SET', last_refill_key, now, 'EX', 3600)
                return {1, math.floor(new_tokens - 1)}
            else
                redis.call('SET', last_refill_key, now, 'EX', 3600)
                return {0, 0}
            end
            """;

    private static final int MAX_CAPACITY = 10;
    private static final int REFILL_RATE = 1; // tokens per second

    public boolean isAllowed(String clientId) {
        String tokensKey = "tb:" + clientId + ":tokens";
        String lastRefillKey = "tb:" + clientId + ":last_refill";
        long now = System.currentTimeMillis() / 1000;

        RedisScript<List> script = RedisScript.of(TOKEN_BUCKET_SCRIPT, List.class);

        List<Long> result = redisTemplate.execute(
                script,
                List.of(tokensKey, lastRefillKey),
                String.valueOf(MAX_CAPACITY),
                String.valueOf(REFILL_RATE),
                String.valueOf(now)
        );

        boolean allowed = result != null && result.get(0) == 1L;
        long remaining = result != null ? result.get(1) : 0;

        log.info("TokenBucket — clientId={}, allowed={}, remaining={}",
                clientId, allowed, remaining);

        return allowed;
    }
}