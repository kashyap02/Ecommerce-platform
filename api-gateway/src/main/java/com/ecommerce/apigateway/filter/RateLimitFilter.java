package com.ecommerce.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class RateLimitFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;

    public RateLimitFilter(
            @Value("${services.rate-limiter.url}") String rateLimiterUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(rateLimiterUrl)
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // Use user email if authenticated, otherwise use IP
        String clientId = exchange.getRequest()
                .getHeaders()
                .getFirst("X-User-Email");

        if (clientId == null) {
            clientId = exchange.getRequest()
                    .getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "anonymous";
        }

        final String finalClientId = clientId;

        return webClient.post()
                .uri("/api/rate-limiter/check")
                .bodyValue(Map.of(
                        "clientId", finalClientId,
                        "algorithm", "sliding-window"
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    Boolean allowed = (Boolean) response.get("allowed");
                    if (Boolean.TRUE.equals(allowed)) {
                        return chain.filter(exchange);
                    } else {
                        log.warn("Rate limit exceeded for clientId={}", finalClientId);
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        return exchange.getResponse().setComplete();
                    }
                })
                .onErrorResume(ex -> {
                    // If Rate Limiter is down, allow request through
                    // Fail open — better to allow than block legitimate traffic
                    log.error("Rate Limiter Service unreachable: {}", ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return 0; // Run after JWT filter (-1)
    }
}