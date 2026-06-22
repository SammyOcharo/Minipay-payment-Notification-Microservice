package com.minipay.notification_service.controller;

import com.minipay.notification_service.dto.NotificationResponse;
import com.minipay.notification_service.enums.NotificationStatus;
import com.minipay.notification_service.enums.NotificationType;
import com.minipay.notification_service.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationController(notificationService);
    }

    // -----------------------------------------------------------------------
    // GET /payment/{paymentId} — happy path
    // -----------------------------------------------------------------------

    @Test
    void getByPaymentId_shouldReturn200WithBody_whenPaymentExists() {
        NotificationResponse notification = buildNotificationResponse("PAY-001", "user@example.com");
        when(notificationService.getNotificationByPaymentId("PAY-001"))
                .thenReturn(notification);

        ResponseEntity<NotificationResponse> response = controller.getByPaymentId("PAY-001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(notification);
        verify(notificationService).getNotificationByPaymentId("PAY-001");
    }

    @Test
    void getByPaymentId_shouldReturnExactServiceResponse_withoutTransformation() {
        NotificationResponse notification = buildNotificationResponse("PAY-001", "user@example.com");
        when(notificationService.getNotificationByPaymentId("PAY-001"))
                .thenReturn(notification);

        ResponseEntity<NotificationResponse> response = controller.getByPaymentId("PAY-001");

        assertThat(response.getBody()).isSameAs(notification);
    }

    @Test
    void getByPaymentId_shouldInvokeServiceExactlyOnce() {
        NotificationResponse notification = buildNotificationResponse("PAY-001", "user@example.com");
        when(notificationService.getNotificationByPaymentId("PAY-001"))
                .thenReturn(notification);

        controller.getByPaymentId("PAY-001");

        verify(notificationService, times(1)).getNotificationByPaymentId("PAY-001");
        verifyNoMoreInteractions(notificationService);
    }

    // -----------------------------------------------------------------------
    // GET /payment/{paymentId} — service throws
    // -----------------------------------------------------------------------

    @Test
    void getByPaymentId_shouldPropagateException_whenServiceThrows() {
        when(notificationService.getNotificationByPaymentId("PAY-999"))
                .thenThrow(new RuntimeException("Notification not found"));

        assertThatCode(() -> controller.getByPaymentId("PAY-999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Notification not found");

        verify(notificationService).getNotificationByPaymentId("PAY-999");
    }

    // -----------------------------------------------------------------------
    // GET /user/{email} — happy path
    // -----------------------------------------------------------------------

    @Test
    void getByEmail_shouldReturn200WithPageBody_whenNotificationsExist() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt"));
        List<NotificationResponse> notifications = List.of(
                buildNotificationResponse("PAY-001", "user@example.com"),
                buildNotificationResponse("PAY-002", "user@example.com")
        );
        Page<NotificationResponse> page = new PageImpl<>(notifications, pageable, 2);
        when(notificationService.getNotificationsByEmail("user@example.com", pageable))
                .thenReturn(page);

        ResponseEntity<Page<NotificationResponse>> response =
                controller.getByEmail("user@example.com", pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(page);
        assertThat(response.getBody().getContent()).hasSize(2);
        verify(notificationService).getNotificationsByEmail("user@example.com", pageable);
    }

    @Test
    void getByEmail_shouldReturnExactServiceResponse_withoutTransformation() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt"));
        Page<NotificationResponse> page = new PageImpl<>(List.of(), pageable, 0);
        when(notificationService.getNotificationsByEmail("user@example.com", pageable))
                .thenReturn(page);

        ResponseEntity<Page<NotificationResponse>> response =
                controller.getByEmail("user@example.com", pageable);

        assertThat(response.getBody()).isSameAs(page);
    }

    @Test
    void getByEmail_shouldReturnEmptyPage_whenNoNotificationsExist() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt"));
        Page<NotificationResponse> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(notificationService.getNotificationsByEmail("user@example.com", pageable))
                .thenReturn(emptyPage);

        ResponseEntity<Page<NotificationResponse>> response =
                controller.getByEmail("user@example.com", pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
    }

    @Test
    void getByEmail_shouldInvokeServiceExactlyOnce() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt"));
        Page<NotificationResponse> page = new PageImpl<>(List.of(), pageable, 0);
        when(notificationService.getNotificationsByEmail("user@example.com", pageable))
                .thenReturn(page);

        controller.getByEmail("user@example.com", pageable);

        verify(notificationService, times(1))
                .getNotificationsByEmail("user@example.com", pageable);
        verifyNoMoreInteractions(notificationService);
    }

    // -----------------------------------------------------------------------
    // GET /user/{email} — pagination metadata preserved
    // -----------------------------------------------------------------------

    @Test
    void getByEmail_shouldPreservePaginationMetadata() {
        Pageable pageable = PageRequest.of(2, 5, Sort.by("createdAt"));
        List<NotificationResponse> notifications = List.of(
                buildNotificationResponse("PAY-010", "user@example.com")
        );
        Page<NotificationResponse> page = new PageImpl<>(notifications, pageable, 11);
        when(notificationService.getNotificationsByEmail("user@example.com", pageable))
                .thenReturn(page);

        ResponseEntity<Page<NotificationResponse>> response =
                controller.getByEmail("user@example.com", pageable);

        assertThat(response.getBody().getTotalElements()).isEqualTo(11);
        assertThat(response.getBody().getTotalPages()).isEqualTo(3);
        assertThat(response.getBody().getNumber()).isEqualTo(2);
        assertThat(response.getBody().getSize()).isEqualTo(5);
    }

    // -----------------------------------------------------------------------
    // GET /user/{email} — service throws
    // -----------------------------------------------------------------------

    @Test
    void getByEmail_shouldPropagateException_whenServiceThrows() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt"));
        when(notificationService.getNotificationsByEmail("ghost@example.com", pageable))
                .thenThrow(new RuntimeException("User not found"));

        assertThatCode(() -> controller.getByEmail("ghost@example.com", pageable))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(notificationService).getNotificationsByEmail("ghost@example.com", pageable);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private NotificationResponse buildNotificationResponse(String paymentId, String email) {
        return NotificationResponse.builder()
                .id("NOTIF-001")
                .paymentId(paymentId)
                .phoneNumber("+254712345678")
                .message("Payment processed successfully")
                .type(NotificationType.PAYMENT_SUCCESS)
                .status(NotificationStatus.SENT)
                .providerMessageId("MSG-001")
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .sentAt(LocalDateTime.now())
                .build();
    }
}