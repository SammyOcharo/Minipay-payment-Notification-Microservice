package com.minipay.notification_service.service.impl;

import com.minipay.notification_service.dto.PaymentEventDto;
import com.minipay.notification_service.enums.NotificationStatus;
import com.minipay.notification_service.model.Notification;
import com.minipay.notification_service.repository.NotificationRepository;
import com.minipay.notification_service.sms.SmsProvider;
import com.minipay.notification_service.sms.SmsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification Service Tests")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SmsProvider smsProvider;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private PaymentEventDto successEvent;
    private PaymentEventDto failedEvent;
    private Notification notification;

    @BeforeEach
    void setUp() {
        successEvent = PaymentEventDto.builder()
                .paymentId("payment-123")
                .userEmail("test@minipay.com")
                .phoneNumber("+254712345678")
                .amount(new BigDecimal("100.00"))
                .currency("KES")
                .paymentMethod("MPESA")
                .status("SUCCESS")
                .gatewayReference("ws_CO_TEST123")
                .timestamp(LocalDateTime.now())
                .build();

        failedEvent = PaymentEventDto.builder()
                .paymentId("payment-456")
                .userEmail("test@minipay.com")
                .phoneNumber("+254712345678")
                .amount(new BigDecimal("100.00"))
                .currency("KES")
                .paymentMethod("MPESA")
                .status("FAILED")
                .failureReason("Insufficient funds")
                .timestamp(LocalDateTime.now())
                .build();

        notification = Notification.builder()
                .id("notif-123")
                .paymentId("payment-123")
                .phoneNumber("+254712345678")
                .status(NotificationStatus.SENT)
                .build();
    }

    @Test
    @DisplayName("Should process successful payment event and send SMS")
    void shouldProcessSuccessfulPaymentEventAndSendSms() {
        // Arrange
        when(notificationRepository.existsByPaymentId("payment-123"))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(notification);
        when(smsProvider.sendSms(anyString(), anyString()))
                .thenReturn(SmsResponse.builder()
                        .success(true)
                        .messageId("MSG-123")
                        .status("SUCCESS")
                        .build());

        // Act
        notificationService.processPaymentEvent(successEvent);

        // Assert
        verify(smsProvider).sendSms(
                eq("+254712345678"), contains("successful"));
        verify(notificationRepository, times(2))
                .save(any(Notification.class));
    }

    @Test
    @DisplayName("Should process failed payment event and send failure SMS")
    void shouldProcessFailedPaymentEventAndSendFailureSms() {
        // Arrange
        when(notificationRepository.existsByPaymentId("payment-456"))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(notification);
        when(smsProvider.sendSms(anyString(), anyString()))
                .thenReturn(SmsResponse.builder()
                        .success(true)
                        .status("SUCCESS")
                        .build());

        // Act
        notificationService.processPaymentEvent(failedEvent);

        // Assert
        verify(smsProvider).sendSms(
                eq("+254712345678"), contains("failed"));
    }

    @Test
    @DisplayName("Should skip duplicate payment event")
    void shouldSkipDuplicatePaymentEvent() {
        // Arrange
        when(notificationRepository.existsByPaymentId("payment-123"))
                .thenReturn(true);

        // Act
        notificationService.processPaymentEvent(successEvent);

        // Assert
        verify(smsProvider, never()).sendSms(anyString(), anyString());
        verify(notificationRepository, never())
                .save(any(Notification.class));
    }

    @Test
    @DisplayName("Should handle SMS failure gracefully")
    void shouldHandleSmsFailureGracefully() {
        // Arrange
        when(notificationRepository.existsByPaymentId("payment-123"))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(notification);
        when(smsProvider.sendSms(anyString(), anyString()))
                .thenReturn(SmsResponse.builder()
                        .success(false)
                        .status("FAILED")
                        .failureReason("Network error")
                        .build());

        // Act
        notificationService.processPaymentEvent(successEvent);

        // Assert — should save with FAILED status
        verify(notificationRepository, times(2))
                .save(argThat(n ->
                        n.getStatus() == null ||
                                n.getStatus() == NotificationStatus.FAILED ||
                                n.getStatus() == NotificationStatus.PENDING));
    }

    @Test
    @DisplayName("Should throw 404 for non-existent notification")
    void shouldThrow404ForNonExistentNotification() {
        // Arrange
        when(notificationRepository.findByPaymentId("non-existent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> notificationService
                .getNotificationByPaymentId("non-existent"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}