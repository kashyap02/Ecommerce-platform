package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.event.PaymentEvent;
import com.ecommerce.orderservice.external.ProductServiceClient;
import com.ecommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderRepository orderRepository;
    private final ProductServiceClient productServiceClient;

    @KafkaListener(
            topics = "${kafka.topics.payment-confirmed}",
            groupId = "${kafka.consumer.group-id}"
    )
    @Transactional
    public void handlePaymentConfirmed(@Payload PaymentEvent event) {
        log.info("Received payment-confirmed for orderId={}", event.getOrderId());

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {

            // Confirm stock deduction in Product Service
            order.getItems().forEach(item ->
                    productServiceClient.confirmStock(item.getProductId(), item.getQuantity())
            );

            // Update order status
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            log.info("Order id={} marked as CONFIRMED", order.getId());
        });
    }

    @KafkaListener(
            topics = "${kafka.topics.payment-failed}",
            groupId = "${kafka.consumer.group-id}"
    )
    @Transactional
    public void handlePaymentFailed(@Payload PaymentEvent event) {
        log.info("Received payment-failed for orderId={}", event.getOrderId());

        orderRepository.findById(event.getOrderId()).ifPresent(order -> {

            // Release reserved stock back to available pool
            order.getItems().forEach(item ->
                    productServiceClient.releaseStock(item.getProductId(), item.getQuantity())
            );

            // Update order status
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);

            log.info("Order id={} marked as FAILED", order.getId());
        });
    }
}