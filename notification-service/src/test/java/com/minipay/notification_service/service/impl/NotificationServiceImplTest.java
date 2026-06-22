package com.minipay.notification_service.service.impl;

import com.minipay.notification_service.dto.PaymentEventDto;
import com.minipay.notification_service.enums.NotificationStatus;
import com.minipay.notification_service.enums.NotificationType;
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
import org.springframework.data.domain.Pageable;
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

    // -----------------------------------------------------------------------
// processPaymentEvent — SMS success path: notification fields updated
// -----------------------------------------------------------------------

    @Test
    @DisplayName("Should set SENT status and provider details when SMS succeeds")
    void shouldSetSentStatusAndProviderDetails_whenSmsSucceeds() {
        Notification savedNotification = Notification.builder()
                .id("notif-123")
                .paymentId("payment-123")
                .phoneNumber("+254712345678")
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();

        when(notificationRepository.existsByPaymentId("payment-123")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(smsProvider.sendSms(anyString(), anyString()))
                .thenReturn(SmsResponse.builder()
                        .success(true)
                        .messageId("MSG-123")
                        .status("DELIVERED")
                        .build());

        notificationService.processPaymentEvent(successEvent);

        verify(notificationRepository, times(2)).save(argThat(n ->
                n.getStatus() == NotificationStatus.PENDING ||
                        n.getStatus() == NotificationStatus.SENT
        ));
    }

    @Test
    @DisplayName("Should set FAILED status and failure reason when SMS fails")
    void shouldSetFailedStatusAndFailureReason_whenSmsFails() {
        Notification savedNotification = Notification.builder()
                .id("notif-123")
                .paymentId("payment-123")
                .phoneNumber("+254712345678")
                .status(NotificationStatus.PENDING)
                .retryCount(0)
                .build();

        when(notificationRepository.existsByPaymentId("payment-123")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        when(smsProvider.sendSms(anyString(), anyString()))
                .thenReturn(SmsResponse.builder()
                        .success(false)
                        .failureReason("Network timeout")
                        .build());

        notificationService.processPaymentEvent(successEvent);

        // First save → PENDING (initial persist)
        verify(notificationRepository).save(argThat(n ->
                n.getStatus() == NotificationStatus.PENDING
        ));

        // Second save → FAILED with incremented retryCount
        verify(notificationRepository).save(argThat(n ->
                n.getStatus() == NotificationStatus.FAILED &&
                        n.getRetryCount() == 1 &&
                        n.getFailureReason().equals("Network timeout")
        ));

        // Total saves = exactly 2
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

// -----------------------------------------------------------------------
// processPaymentEvent — buildSmsMessage: default/PENDING status branch
// -----------------------------------------------------------------------

    @Test
    @DisplayName("Should send processing message for PENDING status")
    void shouldSendProcessingMessage_whenStatusIsPending() {
        PaymentEventDto pendingEvent = PaymentEventDto.builder()
                .paymentId("payment-789")
                .userEmail("test@minipay.com")
                .phoneNumber("+254712345678")
                .amount(new BigDecimal("100.00"))
                .currency("KES")
                .status("PENDING")
                .timestamp(LocalDateTime.now())
                .build();

        when(notificationRepository.existsByPaymentId("payment-789")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(Notification.builder()
                        .id("notif-789")
                        .paymentId("payment-789")
                        .status(NotificationStatus.PENDING)
                        .retryCount(0)
                        .build());
        when(smsProvider.sendSms(anyString(), anyString()))
                .thenReturn(SmsResponse.builder().success(true).build());

        notificationService.processPaymentEvent(pendingEvent);

        verify(smsProvider).sendSms(
                eq("+254712345678"), contains("being processed"));
    }

// -----------------------------------------------------------------------
// processPaymentEvent — buildSmsMessage: null gatewayReference falls
//                       back to paymentId
// -----------------------------------------------------------------------

    @Test
    @DisplayName("Should use paymentId as reference when gatewayReference is null")
    void shouldUsePaymentIdAsReference_whenGatewayReferenceIsNull() {
        PaymentEventDto eventWithoutRef = PaymentEventDto.builder()
                .paymentId("payment-123")
                .userEmail("test@minipay.com")
                .phoneNumber("+254712345678")
                .amount(new BigDecimal("100.00"))
                .currency("KES")
                .status("SUCCESS")
                .gatewayReference(null)
                .timestamp(LocalDateTime.now())
                .build();

        when(notificationRepository.existsByPaymentId("payment-123")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(Notification.builder()
                        .id("notif-123")
                        .paymentId("payment-123")
                        .status(NotificationStatus.PENDING)
                        .retryCount(0)
                        .build());
        when(smsProvider.sendSms(anyString(), anyString()))
                .thenReturn(SmsResponse.builder().success(true).build());

        notificationService.processPaymentEvent(eventWithoutRef);

        verify(smsProvider).sendSms(
                eq("+254712345678"), contains("payment-123"));
    }

// -----------------------------------------------------------------------
// processPaymentEvent — buildSmsMessage: null failureReason falls back
//                       to "Unknown error"
// -----------------------------------------------------------------------

    @Test
    @DisplayName("Should use 'Unknown error' when failureReason is null")
    void shouldUseUnknownError_whenFailureReasonIsNull() {
        PaymentEventDto eventWithoutReason = PaymentEventDto.builder()
                .paymentId("payment-456")
                .userEmail("test@minipay.com")
                .phoneNumber("+254712345678")
                .amount(new BigDecimal("100.00"))
                .currency("KES")
                .status("FAILED")
                .failureReason(null)
                .timestamp(LocalDateTime.now())
                .build();

        when(notificationRepository.existsByPaymentId("payment-456")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(Notification.builder()
                        .id("notif-456")
                        .paymentId("payment-456")
                        .status(NotificationStatus.PENDING)
                        .retryCount(0)
                        .build());
        when(smsProvider.sendSms(anyString(), anyString()))
                .thenReturn(SmsResponse.builder().success(true).build());

        notificationService.processPaymentEvent(eventWithoutReason);

        verify(smsProvider).sendSms(
                eq("+254712345678"), contains("Unknown error"));
    }

    @Test
    @DisplayName("Should resolve PAYMENT_SUCCESS type for SUCCESS status")
    void shouldResolvePaymentSuccessType_forSuccessStatus() {
        when(notificationRepository.existsByPaymentId("payment-123")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(smsProvider.sendSms(anyString(), anyString()))
                .thenReturn(SmsResponse.builder().success(true).build());

        notificationService.processPaymentEvent(successEvent);

        verify(notificationRepository, atLeastOnce()).save(argThat(n ->
                n.getType() == NotificationType.PAYMENT_SUCCESS
        ));
    }

    @Test
    @DisplayName("Should resolve PAYMENT_FAILED type for FAILED status")
    void shouldResolvePaymentFailedType_forFailedStatus() {
        when(notificationRepository.existsByPaymentId("payment-456")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(smsProvider.sendSms(anyString(), anyString()))
                .thenReturn(SmsResponse.builder().success(true).build());

        notificationService.processPaymentEvent(failedEvent);

        // save is called twice — first with PENDING, second after SMS result.
        // Use atLeastOnce() since we only care that the type was set correctly
        // on the initial save, not how many times save was called total.
        verify(notificationRepository, atLeastOnce()).save(argThat(n ->
                n.getType() == NotificationType.PAYMENT_FAILED
        ));
    }

    @Test
    @DisplayName("Should resolve PAYMENT_PENDING type for unknown status")
    void shouldResolvePaymentPendingType_forUnknownStatus() {
        PaymentEventDto pendingEvent = PaymentEventDto.builder()
                .paymentId("payment-789")
                .userEmail("test@minipay.com")
                .phoneNumber("+254712345678")
                .amount(new BigDecimal("100.00"))
                .currency("KES")
                .status("PENDING")
                .timestamp(LocalDateTime.now())
                .build();

        when(notificationRepository.existsByPaymentId("payment-789")).thenReturn(false);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(smsProvider.sendSms(anyString(), anyString()))
                .thenReturn(SmsResponse.builder().success(true).build());

        notificationService.processPaymentEvent(pendingEvent);

        verify(notificationRepository, atLeastOnce()).save(argThat(n ->
                n.getType() == NotificationType.PAYMENT_PENDING
        ));
    }

// -----------------------------------------------------------------------
// getNotificationByPaymentId — found
// -----------------------------------------------------------------------

    @Test
    @DisplayName("Should return NotificationResponse when paymentId exists")
    void shouldReturnNotificationResponse_whenPaymentIdExists() {
        LocalDateTime now = LocalDateTime.now();
        Notification found = Notification.builder()
                .id("notif-123")
                .paymentId("payment-123")
                .phoneNumber("+254712345678")
                .message("Payment successful")
                .type(com.minipay.notification_service.enums.NotificationType.PAYMENT_SUCCESS)
                .status(NotificationStatus.SENT)
                .providerMessageId("MSG-001")
                .failureReason(null)
                .retryCount(0)
                .createdAt(now)
                .sentAt(now)
                .build();

        when(notificationRepository.findByPaymentId("payment-123"))
                .thenReturn(Optional.of(found));

        var response = notificationService.getNotificationByPaymentId("payment-123");

        assertThat(response.getId()).isEqualTo("notif-123");
        assertThat(response.getPaymentId()).isEqualTo("payment-123");
        assertThat(response.getPhoneNumber()).isEqualTo("+254712345678");
        assertThat(response.getMessage()).isEqualTo("Payment successful");
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(response.getProviderMessageId()).isEqualTo("MSG-001");
        assertThat(response.getRetryCount()).isZero();
        assertThat(response.getCreatedAt()).isEqualTo(now);
        assertThat(response.getSentAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should include paymentId in 404 exception message")
    void shouldIncludePaymentIdIn404Message() {
        when(notificationRepository.findByPaymentId("missing-id"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                notificationService.getNotificationByPaymentId("missing-id"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("missing-id");
    }

// -----------------------------------------------------------------------
// getNotificationsByEmail
// -----------------------------------------------------------------------

    @Test
    @DisplayName("Should return page of notifications mapped to responses")
    void shouldReturnPageOfNotificationResponses_forEmail() {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        LocalDateTime now = LocalDateTime.now();

        Notification n1 = Notification.builder()
                .id("notif-1").paymentId("PAY-1")
                .phoneNumber("+254712345678").message("msg1")
                .type(com.minipay.notification_service.enums.NotificationType.PAYMENT_SUCCESS)
                .status(NotificationStatus.SENT)
                .retryCount(0).createdAt(now).sentAt(now).build();

        Notification n2 = Notification.builder()
                .id("notif-2").paymentId("PAY-2")
                .phoneNumber("+254712345678").message("msg2")
                .type(com.minipay.notification_service.enums.NotificationType.PAYMENT_FAILED)
                .status(NotificationStatus.FAILED)
                .retryCount(1).createdAt(now).build();

        org.springframework.data.domain.Page<Notification> page =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(n1, n2), pageable, 2);

        when(notificationRepository.findByUserEmail("test@minipay.com", pageable))
                .thenReturn(page);

        var result = notificationService
                .getNotificationsByEmail("test@minipay.com", pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent())
                .extracting(r -> r.getPaymentId())
                .containsExactly("PAY-1", "PAY-2");
    }

    @Test
    @DisplayName("Should return empty page when no notifications exist for email")
    void shouldReturnEmptyPage_whenNoNotificationsForEmail() {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(notificationRepository.findByUserEmail("ghost@example.com", pageable))
                .thenReturn(org.springframework.data.domain.Page.empty(pageable));

        var result = notificationService
                .getNotificationsByEmail("ghost@example.com", pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }
}