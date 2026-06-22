package com.minipay.notification_service.model;

import com.minipay.notification_service.enums.NotificationStatus;
import com.minipay.notification_service.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTest {

    // -----------------------------------------------------------------------
    // No-args constructor
    // -----------------------------------------------------------------------

    @Test
    void noArgsConstructor_shouldCreateInstanceWithNullFields() {
        Notification notification = new Notification();

        assertThat(notification).isNotNull();
        assertThat(notification.getId()).isNull();
        assertThat(notification.getPaymentId()).isNull();
        assertThat(notification.getPhoneNumber()).isNull();
        assertThat(notification.getUserEmail()).isNull();
        assertThat(notification.getMessage()).isNull();
        assertThat(notification.getType()).isNull();
        assertThat(notification.getProviderMessageId()).isNull();
        assertThat(notification.getProviderStatus()).isNull();
        assertThat(notification.getFailureReason()).isNull();
        assertThat(notification.getSentAt()).isNull();
    }

    // -----------------------------------------------------------------------
    // Builder — defaults
    // -----------------------------------------------------------------------

    @Test
    void builder_shouldSetDefaultStatus_toPending() {
        Notification notification = Notification.builder()
                .paymentId("PAY-001")
                .phoneNumber("+254712345678")
                .userEmail("user@example.com")
                .message("Payment received")
                .type(NotificationType.PAYMENT_SUCCESS)
                .build();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void builder_shouldSetDefaultRetryCount_toZero() {
        Notification notification = Notification.builder()
                .paymentId("PAY-001")
                .phoneNumber("+254712345678")
                .userEmail("user@example.com")
                .message("Payment received")
                .type(NotificationType.PAYMENT_SUCCESS)
                .build();

        assertThat(notification.getRetryCount()).isZero();
    }

    // -----------------------------------------------------------------------
    // Builder — explicit values override defaults
    // -----------------------------------------------------------------------

    @Test
    void builder_shouldAllowOverridingDefaultStatus() {
        Notification notification = Notification.builder()
                .paymentId("PAY-001")
                .phoneNumber("+254712345678")
                .userEmail("user@example.com")
                .message("Payment received")
                .type(NotificationType.PAYMENT_SUCCESS)
                .status(NotificationStatus.SENT)
                .build();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void builder_shouldAllowOverridingDefaultRetryCount() {
        Notification notification = Notification.builder()
                .paymentId("PAY-001")
                .phoneNumber("+254712345678")
                .userEmail("user@example.com")
                .message("Payment received")
                .type(NotificationType.PAYMENT_SUCCESS)
                .retryCount(3)
                .build();

        assertThat(notification.getRetryCount()).isEqualTo(3);
    }

    // -----------------------------------------------------------------------
    // Builder — all fields set correctly
    // -----------------------------------------------------------------------

    @Test
    void builder_shouldSetAllFields_correctly() {
        LocalDateTime now = LocalDateTime.now();

        Notification notification = Notification.builder()
                .id("NOTIF-001")
                .paymentId("PAY-001")
                .phoneNumber("+254712345678")
                .userEmail("user@example.com")
                .message("Payment received")
                .type(NotificationType.PAYMENT_SUCCESS)
                .status(NotificationStatus.SENT)
                .providerMessageId("MSG-001")
                .providerStatus("Success")
                .failureReason(null)
                .retryCount(1)
                .createdAt(now)
                .updatedAt(now)
                .sentAt(now)
                .build();

        assertThat(notification.getId()).isEqualTo("NOTIF-001");
        assertThat(notification.getPaymentId()).isEqualTo("PAY-001");
        assertThat(notification.getPhoneNumber()).isEqualTo("+254712345678");
        assertThat(notification.getUserEmail()).isEqualTo("user@example.com");
        assertThat(notification.getMessage()).isEqualTo("Payment received");
        assertThat(notification.getType()).isEqualTo(NotificationType.PAYMENT_SUCCESS);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getProviderMessageId()).isEqualTo("MSG-001");
        assertThat(notification.getProviderStatus()).isEqualTo("Success");
        assertThat(notification.getFailureReason()).isNull();
        assertThat(notification.getRetryCount()).isEqualTo(1);
        assertThat(notification.getCreatedAt()).isEqualTo(now);
        assertThat(notification.getUpdatedAt()).isEqualTo(now);
        assertThat(notification.getSentAt()).isEqualTo(now);
    }

    // -----------------------------------------------------------------------
    // Setters — all fields mutable via @Setter
    // -----------------------------------------------------------------------

    @Test
    void setters_shouldUpdateAllFields() {
        LocalDateTime now = LocalDateTime.now();
        Notification notification = new Notification();

        notification.setId("NOTIF-001");
        notification.setPaymentId("PAY-001");
        notification.setPhoneNumber("+254712345678");
        notification.setUserEmail("user@example.com");
        notification.setMessage("Payment received");
        notification.setType(NotificationType.PAYMENT_SUCCESS);
        notification.setStatus(NotificationStatus.SENT);
        notification.setProviderMessageId("MSG-001");
        notification.setProviderStatus("Success");
        notification.setFailureReason("None");
        notification.setRetryCount(2);
        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);
        notification.setSentAt(now);

        assertThat(notification.getId()).isEqualTo("NOTIF-001");
        assertThat(notification.getPaymentId()).isEqualTo("PAY-001");
        assertThat(notification.getPhoneNumber()).isEqualTo("+254712345678");
        assertThat(notification.getUserEmail()).isEqualTo("user@example.com");
        assertThat(notification.getMessage()).isEqualTo("Payment received");
        assertThat(notification.getType()).isEqualTo(NotificationType.PAYMENT_SUCCESS);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getProviderMessageId()).isEqualTo("MSG-001");
        assertThat(notification.getProviderStatus()).isEqualTo("Success");
        assertThat(notification.getFailureReason()).isEqualTo("None");
        assertThat(notification.getRetryCount()).isEqualTo(2);
        assertThat(notification.getCreatedAt()).isEqualTo(now);
        assertThat(notification.getUpdatedAt()).isEqualTo(now);
        assertThat(notification.getSentAt()).isEqualTo(now);
    }

    // -----------------------------------------------------------------------
    // AllArgsConstructor
    // -----------------------------------------------------------------------

    @Test
    void allArgsConstructor_shouldSetAllFields() {
        LocalDateTime now = LocalDateTime.now();

        Notification notification = new Notification(
                "NOTIF-001",
                "PAY-001",
                "+254712345678",
                "user@example.com",
                "Payment received",
                NotificationType.PAYMENT_SUCCESS,
                NotificationStatus.SENT,
                "MSG-001",
                "Success",
                null,
                1,
                now,
                now,
                now
        );

        assertThat(notification.getId()).isEqualTo("NOTIF-001");
        assertThat(notification.getPaymentId()).isEqualTo("PAY-001");
        assertThat(notification.getPhoneNumber()).isEqualTo("+254712345678");
        assertThat(notification.getUserEmail()).isEqualTo("user@example.com");
        assertThat(notification.getMessage()).isEqualTo("Payment received");
        assertThat(notification.getType()).isEqualTo(NotificationType.PAYMENT_SUCCESS);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getProviderMessageId()).isEqualTo("MSG-001");
        assertThat(notification.getProviderStatus()).isEqualTo("Success");
        assertThat(notification.getFailureReason()).isNull();
        assertThat(notification.getRetryCount()).isEqualTo(1);
        assertThat(notification.getCreatedAt()).isEqualTo(now);
        assertThat(notification.getUpdatedAt()).isEqualTo(now);
        assertThat(notification.getSentAt()).isEqualTo(now);
    }

    // -----------------------------------------------------------------------
    // Enum values — NotificationStatus
    // -----------------------------------------------------------------------

    @Test
    void status_shouldAcceptPendingValue() {
        Notification notification = Notification.builder()
                .paymentId("PAY-001")
                .phoneNumber("+254712345678")
                .userEmail("user@example.com")
                .message("Payment received")
                .type(NotificationType.PAYMENT_SUCCESS)
                .status(NotificationStatus.PENDING)
                .build();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void status_shouldAcceptFailedValue() {
        Notification notification = Notification.builder()
                .paymentId("PAY-001")
                .phoneNumber("+254712345678")
                .userEmail("user@example.com")
                .message("Payment received")
                .type(NotificationType.PAYMENT_SUCCESS)
                .status(NotificationStatus.FAILED)
                .build();

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    // -----------------------------------------------------------------------
    // Retry count — increments correctly via setter
    // -----------------------------------------------------------------------

    @Test
    void retryCount_shouldIncrementCorrectly() {
        Notification notification = Notification.builder()
                .paymentId("PAY-001")
                .phoneNumber("+254712345678")
                .userEmail("user@example.com")
                .message("Payment received")
                .type(NotificationType.PAYMENT_SUCCESS)
                .build();

        assertThat(notification.getRetryCount()).isZero();

        notification.setRetryCount(notification.getRetryCount() + 1);
        assertThat(notification.getRetryCount()).isEqualTo(1);

        notification.setRetryCount(notification.getRetryCount() + 1);
        assertThat(notification.getRetryCount()).isEqualTo(2);
    }
}