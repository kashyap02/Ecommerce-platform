package com.ecommerce.ratelimiterservice.dto;

import lombok.Data;

@Data
public class RateLimitRequest {
    private String clientId;  // IP address or user email
    private String algorithm; // "token-bucket" or "sliding-window"
}