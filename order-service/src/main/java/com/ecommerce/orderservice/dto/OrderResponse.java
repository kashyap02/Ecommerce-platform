package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.entity.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class OrderResponse {

    private Long id;
    private String userEmail;
    private String status;
    private BigDecimal totalAmount;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userEmail(order.getUserEmail())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .items(order.getItems().stream()
                        .map(OrderItemResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}