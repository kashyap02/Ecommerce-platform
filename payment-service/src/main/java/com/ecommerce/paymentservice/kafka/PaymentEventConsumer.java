package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.event.OrderPlacedEvent;
import com.ecommerce.paymentservice.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final PaymentEventProducer paymentEventProducer;
    private final Random random = new Random();

    @KafkaListener(
            topics = "${kafka.topics.order-placed}",
            groupId = "${kafka.consumer.group-id}"
    )
    public void handleOrderPlaced(@Payload OrderPlacedEvent event) {
        log.info("Received order-placed for orderId={}, amount={}",
                event.getOrderId(), event.getTotalAmount());

        try {
            // Simulate payment processing latency
            Thread.sleep(1000);

            /*
             * Simulate payment gateway response.
             * 80% success rate — realistic enough to test both paths.
             * In production: replace with actual payment gateway call.
             */
            boolean paymentSuccessful = random.nextInt(10) < 8;

            PaymentEvent paymentEvent = PaymentEvent.builder()
                    .orderId(event.getOrderId())
                    .userEmail(event.getUserEmail())
                    .status(paymentSuccessful ? "CONFIRMED" : "FAILED")
                    .build();

            if (paymentSuccessful) {
                log.info("Payment CONFIRMED for orderId={}", event.getOrderId());
                paymentEventProducer.publishPaymentConfirmed(paymentEvent);
            } else {
                log.info("Payment FAILED for orderId={}", event.getOrderId());
                paymentEventProducer.publishPaymentFailed(paymentEvent);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted for orderId={}", event.getOrderId());

            // Publish failure on interruption — never leave an order in PENDING forever
            paymentEventProducer.publishPaymentFailed(PaymentEvent.builder()
                    .orderId(event.getOrderId())
                    .userEmail(event.getUserEmail())
                    .status("FAILED")
                    .build());
        }
    }
}