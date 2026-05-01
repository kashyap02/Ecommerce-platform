package com.ecommerce.notificationservice.kafka;

import com.ecommerce.notificationservice.event.PaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationEventConsumer {

    @KafkaListener(
            topics = "${kafka.topics.payment-confirmed}",
            groupId = "${kafka.consumer.group-id}"
    )
    public void handlePaymentConfirmed(@Payload PaymentEvent event) {
        log.info("======================================");
        log.info("NOTIFICATION — Payment Confirmed");
        log.info("Order ID  : {}", event.getOrderId());
        log.info("Email     : {}", event.getUserEmail());
        log.info("Message   : Your order #{} has been confirmed and is being processed.", event.getOrderId());
        log.info("======================================");

        // In production: emailService.send(event.getUserEmail(), "Order Confirmed", ...)
    }

    @KafkaListener(
            topics = "${kafka.topics.payment-failed}",
            groupId = "${kafka.consumer.group-id}"
    )
    public void handlePaymentFailed(@Payload PaymentEvent event) {
        log.info("======================================");
        log.info("NOTIFICATION — Payment Failed");
        log.info("Order ID  : {}", event.getOrderId());
        log.info("Email     : {}", event.getUserEmail());
        log.info("Message   : Your order #{} could not be processed. Please try again.", event.getOrderId());
        log.info("======================================");

        // In production: emailService.send(event.getUserEmail(), "Payment Failed", ...)
    }
}