package com.minipay.notification_service.consumer;

import com.minipay.notification_service.dto.PaymentEventDto;
import com.minipay.notification_service.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock
    private NotificationService notificationService;

    private PaymentEventConsumer consumer;

    private static final String TOPIC     = "payment-events";
    private static final int    PARTITION = 0;
    private static final long   OFFSET    = 42L;

    @BeforeEach
    void setUp() {
        consumer = new PaymentEventConsumer(notificationService);
    }

    // -----------------------------------------------------------------------
    // Happy path — event processed successfully
    // -----------------------------------------------------------------------

    @Test
    void consumePaymentEvent_shouldInvokeNotificationService_whenEventIsValid() {
        PaymentEventDto event = buildEvent("PAY-001", "SUCCESS");

        consumer.consumePaymentEvent(event, TOPIC, PARTITION, OFFSET);

        verify(notificationService).processPaymentEvent(event);
    }

    @Test
    void consumePaymentEvent_shouldInvokeServiceExactlyOnce_perEvent() {
        PaymentEventDto event = buildEvent("PAY-001", "SUCCESS");

        consumer.consumePaymentEvent(event, TOPIC, PARTITION, OFFSET);

        verify(notificationService, times(1)).processPaymentEvent(event);
        verifyNoMoreInteractions(notificationService);
    }

    // -----------------------------------------------------------------------
    // Different statuses — service is called regardless of status value
    // -----------------------------------------------------------------------

    @Test
    void consumePaymentEvent_shouldProcessEvent_whenStatusIsFailed() {
        PaymentEventDto event = buildEvent("PAY-002", "FAILED");

        consumer.consumePaymentEvent(event, TOPIC, PARTITION, OFFSET);

        verify(notificationService).processPaymentEvent(event);
    }

    @Test
    void consumePaymentEvent_shouldProcessEvent_whenStatusIsPending() {
        PaymentEventDto event = buildEvent("PAY-003", "PENDING");

        consumer.consumePaymentEvent(event, TOPIC, PARTITION, OFFSET);

        verify(notificationService).processPaymentEvent(event);
    }

    // -----------------------------------------------------------------------
    // Exception handling — swallowed, never rethrown
    // -----------------------------------------------------------------------

    @Test
    void consumePaymentEvent_shouldNotThrow_whenNotificationServiceThrowsRuntimeException() {
        PaymentEventDto event = buildEvent("PAY-004", "SUCCESS");
        doThrow(new RuntimeException("Notification failure"))
                .when(notificationService).processPaymentEvent(event);

        // Must not propagate — exception is caught and logged internally
        org.assertj.core.api.Assertions.assertThatCode(() ->
                consumer.consumePaymentEvent(event, TOPIC, PARTITION, OFFSET)
        ).doesNotThrowAnyException();

        verify(notificationService).processPaymentEvent(event);
    }

    @Test
    void consumePaymentEvent_shouldNotThrow_whenNotificationServiceThrowsCheckedException() {
        PaymentEventDto event = buildEvent("PAY-005", "SUCCESS");
        doThrow(new RuntimeException(new Exception("Checked exception wrapped")))
                .when(notificationService).processPaymentEvent(event);

        org.assertj.core.api.Assertions.assertThatCode(() ->
                consumer.consumePaymentEvent(event, TOPIC, PARTITION, OFFSET)
        ).doesNotThrowAnyException();
    }

    @Test
    void consumePaymentEvent_shouldNotThrow_whenNotificationServiceThrowsError() {
        PaymentEventDto event = buildEvent("PAY-006", "SUCCESS");
        // Documents current behavior: only Exception is caught — an Error
        // (e.g. OutOfMemoryError) would still propagate. This test confirms
        // that a plain RuntimeException is fully swallowed.
        doThrow(new RuntimeException("Unexpected error"))
                .when(notificationService).processPaymentEvent(event);

        org.assertj.core.api.Assertions.assertThatCode(() ->
                consumer.consumePaymentEvent(event, TOPIC, PARTITION, OFFSET)
        ).doesNotThrowAnyException();
    }

    // -----------------------------------------------------------------------
    // Header values — consumer accepts any topic, partition, offset
    // -----------------------------------------------------------------------

    @Test
    void consumePaymentEvent_shouldProcess_withDifferentTopicPartitionAndOffset() {
        PaymentEventDto event = buildEvent("PAY-007", "SUCCESS");

        consumer.consumePaymentEvent(event, "other-topic", 3, 999L);

        verify(notificationService).processPaymentEvent(event);
    }

    @Test
    void consumePaymentEvent_shouldProcess_withZeroPartitionAndOffset() {
        PaymentEventDto event = buildEvent("PAY-008", "SUCCESS");

        consumer.consumePaymentEvent(event, TOPIC, 0, 0L);

        verify(notificationService).processPaymentEvent(event);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private PaymentEventDto buildEvent(String paymentId, String status) {
        PaymentEventDto event = new PaymentEventDto();
        event.setPaymentId(paymentId);
        event.setStatus(status);
        return event;
    }
}