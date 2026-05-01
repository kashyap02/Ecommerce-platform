package com.ecommerce.paymentservice.kafka;

import com.ecommerce.paymentservice.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Value("${kafka.topics.payment-confirmed}")
    private String paymentConfirmedTopic;

    @Value("${kafka.topics.payment-failed}")
    private String paymentFailedTopic;

    public void publishPaymentConfirmed(PaymentEvent event) {
        log.info("Publishing payment-confirmed for orderId={}", event.getOrderId());
        kafkaTemplate.send(paymentConfirmedTopic,
                String.valueOf(event.getOrderId()),
                event);
    }

    public void publishPaymentFailed(PaymentEvent event) {
        log.info("Publishing payment-failed for orderId={}", event.getOrderId());
        kafkaTemplate.send(paymentFailedTopic,
                String.valueOf(event.getOrderId()),
                event);
    }
}