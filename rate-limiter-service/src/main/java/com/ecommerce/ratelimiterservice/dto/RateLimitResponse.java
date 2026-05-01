package com.ecommerce.ratelimiterservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResponse {
    private boolean allowed;
    private String algorithm;
    private String clientId;
    private String message;
    private long remainingRequests;
}