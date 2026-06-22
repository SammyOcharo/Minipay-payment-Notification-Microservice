package com.minipay.notification_service.consumer;


import com.minipay.notification_service.dto.PaymentEventDto;
import com.minipay.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${kafka.topics.payment-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePaymentEvent(
            @Payload PaymentEventDto event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received payment event. Topic: {} Partition: {} " +
                        "Offset: {} PaymentId: {} Status: {}",
                topic, partition, offset,
                event.getPaymentId(), event.getStatus());

        try {
            notificationService.processPaymentEvent(event);
        } catch (Exception e) {
            log.error("Failed to process payment event for paymentId: {} " +
                    "error: {}", event.getPaymentId(), e.getMessage(), e);
        }
    }
}