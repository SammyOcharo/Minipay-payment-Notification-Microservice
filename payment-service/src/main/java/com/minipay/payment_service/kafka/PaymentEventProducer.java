package com.minipay.payment_service.kafka;

import com.minipay.payment_service.dto.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Value("${kafka.topics.payment-events}")
    private String paymentEventsTopic;

    public void publishPaymentEvent(PaymentEvent event) {
        log.info("Publishing payment event for paymentId: {} status: {}",
                event.getPaymentId(), event.getStatus());

        CompletableFuture<SendResult<String, PaymentEvent>> future =
                kafkaTemplate.send(
                        paymentEventsTopic,
                        event.getPaymentId(),  // key for partitioning
                        event
                );

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish payment event for paymentId: {} error: {}",
                        event.getPaymentId(), ex.getMessage());
            } else {
                log.info("Payment event published successfully. " +
                                "PaymentId: {} Partition: {} Offset: {}",
                        event.getPaymentId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}