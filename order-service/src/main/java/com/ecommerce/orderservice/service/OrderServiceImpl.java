package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderItemRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.event.OrderPlacedEvent;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.external.ProductServiceClient;
import com.ecommerce.orderservice.kafka.OrderEventProducer;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;
    private final OrderEventProducer orderEventProducer;

    @Override
    @Transactional
    public OrderResponse createOrder(String userEmail, CreateOrderRequest request) {

        // Step 1 — Reserve stock for every item
        // If any reservation fails, ProductServiceException is thrown
        // and the whole method rolls back — no partial orders
        for (OrderItemRequest item : request.getItems()) {
            productServiceClient.reserveStock(item.getProductId(), item.getQuantity());
        }

        // Step 2 — Build order items
        // Price is hardcoded as placeholder here — Payment Service
        // will use totalAmount from the Kafka event
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            // In a real system you'd fetch price from Product Service
            // For now we use a placeholder — Payment Service gets
            // totalAmount from the Kafka event
            BigDecimal price = BigDecimal.valueOf(100.00); // placeholder

            OrderItem item = OrderItem.builder()
                    .productId(itemRequest.getProductId())
                    .productName("Product-" + itemRequest.getProductId()) // placeholder
                    .quantity(itemRequest.getQuantity())
                    .priceAtPurchase(price)
                    .build();

            orderItems.add(item);
            totalAmount = totalAmount.add(
                    price.multiply(BigDecimal.valueOf(itemRequest.getQuantity()))
            );
        }

        // Step 3 — Save order with PENDING status
        Order order = Order.builder()
                .userEmail(userEmail)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .build();

        // Wire items to order (both sides of the relationship)
        orderItems.forEach(item -> item.setOrder(order));
        order.getItems().addAll(orderItems);

        Order saved = orderRepository.save(order);
        log.info("Order id={} saved with status PENDING", saved.getId());

        // Step 4 — Publish order-placed event to Kafka
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .orderId(saved.getId())
                .userEmail(saved.getUserEmail())
                .totalAmount(saved.getTotalAmount())
                .items(saved.getItems().stream()
                        .map(item -> OrderPlacedEvent.OrderItemEvent.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .priceAtPurchase(item.getPriceAtPurchase())
                                .build())
                        .collect(Collectors.toList()))
                .build();

        orderEventProducer.publishOrderPlaced(event);

        return OrderResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, String userEmail) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Users can only see their own orders
        if (!order.getUserEmail().equals(userEmail)) {
            throw new OrderNotFoundException(orderId);
        }

        return OrderResponse.from(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userEmail) {
        return orderRepository.findByUserEmail(userEmail)
                .stream()
                .map(OrderResponse::from)
                .collect(Collectors.toList());
    }
}