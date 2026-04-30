package com.ecommerce.orderservice.kafka;

import com.ecommerce.orderservice.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Value("${kafka.topics.order-placed}")
    private String orderPlacedTopic;

    public void publishOrderPlaced(OrderPlacedEvent event) {
        log.info("Publishing order-placed event for orderId={}", event.getOrderId());

        CompletableFuture<SendResult<String, OrderPlacedEvent>> future =
                kafkaTemplate.send(orderPlacedTopic,
                        String.valueOf(event.getOrderId()),
                        event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish order-placed event for orderId={}: {}",
                        event.getOrderId(), ex.getMessage());
            } else {
                log.info("Successfully published order-placed event for orderId={}, " +
                                "partition={}, offset={}",
                        event.getOrderId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}