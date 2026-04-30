package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(String userEmail, CreateOrderRequest request);

    OrderResponse getOrderById(Long orderId, String userEmail);

    List<OrderResponse> getMyOrders(String userEmail);
}