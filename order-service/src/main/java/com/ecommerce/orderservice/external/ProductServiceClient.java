package com.ecommerce.orderservice.external;

import com.ecommerce.orderservice.exception.ProductServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@Slf4j
public class ProductServiceClient {

    private final WebClient webClient;

    public ProductServiceClient(
            @Value("${services.product-service.url}") String productServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(productServiceUrl)
                .build();
    }

    // ── Called before publishing order-placed ─────────────────────────────────

    public void reserveStock(Long productId, Integer quantity) {
        log.info("Reserving {} units for product id={}", quantity, productId);
        sendStockRequest("/api/products/" + productId + "/reserve", quantity);
    }

    // ── Called on payment-failed event ────────────────────────────────────────

    public void releaseStock(Long productId, Integer quantity) {
        log.info("Releasing {} units for product id={}", quantity, productId);
        sendStockRequest("/api/products/" + productId + "/release", quantity);
    }

    // ── Called on payment-confirmed event ─────────────────────────────────────

    public void confirmStock(Long productId, Integer quantity) {
        log.info("Confirming {} units for product id={}", quantity, productId);
        sendStockRequest("/api/products/" + productId + "/confirm", quantity);
    }

    // ── Private helper ────────────────────────────────────────────────────────

    private void sendStockRequest(String uri, Integer quantity) {
        try {
            webClient.post()
                    .uri(uri)
                    .bodyValue(Map.of("quantity", quantity))
                    .retrieve()
                    .onStatus(
                            status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .map(body -> new ProductServiceException(
                                            "Product Service error: " + body))
                    )
                    .toBodilessEntity()
                    .block(); // blocking call — our service layer is synchronous
        } catch (ProductServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProductServiceException(
                    "Failed to reach Product Service: " + ex.getMessage());
        }
    }
}