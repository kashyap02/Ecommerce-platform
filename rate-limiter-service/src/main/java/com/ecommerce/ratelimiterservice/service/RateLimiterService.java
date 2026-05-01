package com.ecommerce.ratelimiterservice.service;

import com.ecommerce.ratelimiterservice.algorithm.SlidingWindowRateLimiter;
import com.ecommerce.ratelimiterservice.algorithm.TokenBucketRateLimiter;
import com.ecommerce.ratelimiterservice.dto.RateLimitRequest;
import com.ecommerce.ratelimiterservice.dto.RateLimitResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final TokenBucketRateLimiter tokenBucketRateLimiter;
    private final SlidingWindowRateLimiter slidingWindowRateLimiter;

    public RateLimitResponse checkLimit(RateLimitRequest request) {
        String clientId = request.getClientId();
        String algorithm = request.getAlgorithm();

        boolean allowed;

        if ("sliding-window".equalsIgnoreCase(algorithm)) {
            allowed = slidingWindowRateLimiter.isAllowed(clientId);
        } else {
            // Default to token bucket
            allowed = tokenBucketRateLimiter.isAllowed(clientId);
        }

        return RateLimitResponse.builder()
                .allowed(allowed)
                .algorithm(algorithm)
                .clientId(clientId)
                .message(allowed
                        ? "Request allowed"
                        : "Rate limit exceeded. Please slow down.")
                .build();
    }
}