package com.ecommerce.ratelimiterservice.controller;

import com.ecommerce.ratelimiterservice.dto.RateLimitRequest;
import com.ecommerce.ratelimiterservice.dto.RateLimitResponse;
import com.ecommerce.ratelimiterservice.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rate-limiter")
@RequiredArgsConstructor
public class RateLimiterController {

    private final RateLimiterService rateLimiterService;

    @PostMapping("/check")
    public ResponseEntity<RateLimitResponse> checkLimit(
            @RequestBody RateLimitRequest request) {

        RateLimitResponse response = rateLimiterService.checkLimit(request);

        if (!response.isAllowed()) {
            return ResponseEntity.status(429).body(response);
        }

        return ResponseEntity.ok(response);
    }
}