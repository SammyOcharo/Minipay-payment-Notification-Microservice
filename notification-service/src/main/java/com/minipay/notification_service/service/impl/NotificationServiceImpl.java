package com.minipay.notification_service.service.impl;


import com.minipay.notification_service.dto.NotificationResponse;
import com.minipay.notification_service.dto.PaymentEventDto;
import com.minipay.notification_service.enums.NotificationStatus;
import com.minipay.notification_service.enums.NotificationType;
import com.minipay.notification_service.model.Notification;
import com.minipay.notification_service.repository.NotificationRepository;
import com.minipay.notification_service.service.NotificationService;
import com.minipay.notification_service.sms.SmsProvider;
import com.minipay.notification_service.sms.SmsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SmsProvider smsProvider;

    @Override
    @Transactional
    public void processPaymentEvent(PaymentEventDto event) {
        log.info("Processing payment event. PaymentId: {} Status: {}",
                event.getPaymentId(), event.getStatus());

        // ─── Deduplication check ──────────────────────────
        if (notificationRepository.existsByPaymentId(
                event.getPaymentId())) {
            log.warn("Notification already sent for paymentId: {}. " +
                    "Skipping.", event.getPaymentId());
            return;
        }

        // ─── Build SMS message ────────────────────────────
        String message = buildSmsMessage(event);
        NotificationType type = resolveNotificationType(event.getStatus());

        // ─── Save notification record ─────────────────────
        Notification notification = Notification.builder()
                .paymentId(event.getPaymentId())
                .phoneNumber(event.getPhoneNumber())
                .userEmail(event.getUserEmail())
                .message(message)
                .type(type)
                .status(NotificationStatus.PENDING)
                .build();

        notification = notificationRepository.save(notification);

        // ─── Send SMS ─────────────────────────────────────
        SmsResponse smsResponse = smsProvider.sendSms(
                event.getPhoneNumber(), message);

        // ─── Update notification with result ──────────────
        if (smsResponse.isSuccess()) {
            notification.setStatus(NotificationStatus.SENT);
            notification.setProviderMessageId(smsResponse.getMessageId());
            notification.setProviderStatus(smsResponse.getStatus());
            notification.setSentAt(LocalDateTime.now());
            log.info("SMS notification sent successfully for paymentId: {}",
                    event.getPaymentId());
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(smsResponse.getFailureReason());
            notification.setRetryCount(notification.getRetryCount() + 1);
            log.error("SMS notification failed for paymentId: {} reason: {}",
                    event.getPaymentId(), smsResponse.getFailureReason());
        }

        notificationRepository.save(notification);
    }

    @Override
    public NotificationResponse getNotificationByPaymentId(
            String paymentId) {
        Notification notification = notificationRepository
                .findByPaymentId(paymentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notification not found for paymentId: " + paymentId
                ));
        return toResponse(notification);
    }

    @Override
    public Page<NotificationResponse> getNotificationsByEmail(
            String email, Pageable pageable) {
        return notificationRepository
                .findByUserEmail(email, pageable)
                .map(this::toResponse);
    }

    // ─── Private Helpers ──────────────────────────────────

    private String buildSmsMessage(PaymentEventDto event) {
        return switch (event.getStatus()) {
            case "SUCCESS" -> String.format(
                    "MiniPay: Your payment of %s %s was successful. " +
                            "Ref: %s. Thank you!",
                    event.getCurrency(),
                    event.getAmount(),
                    event.getGatewayReference() != null
                            ? event.getGatewayReference() : event.getPaymentId()
            );
            case "FAILED" -> String.format(
                    "MiniPay: Your payment of %s %s failed. " +
                            "Reason: %s. Please try again.",
                    event.getCurrency(),
                    event.getAmount(),
                    event.getFailureReason() != null
                            ? event.getFailureReason() : "Unknown error"
            );
            default -> String.format(
                    "MiniPay: Your payment of %s %s is being processed. " +
                            "We will notify you once complete.",
                    event.getCurrency(),
                    event.getAmount()
            );
        };
    }

    private NotificationType resolveNotificationType(String status) {
        return switch (status) {
            case "SUCCESS" -> NotificationType.PAYMENT_SUCCESS;
            case "FAILED" -> NotificationType.PAYMENT_FAILED;
            default -> NotificationType.PAYMENT_PENDING;
        };
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .paymentId(n.getPaymentId())
                .phoneNumber(n.getPhoneNumber())
                .message(n.getMessage())
                .type(n.getType())
                .status(n.getStatus())
                .providerMessageId(n.getProviderMessageId())
                .failureReason(n.getFailureReason())
                .retryCount(n.getRetryCount())
                .createdAt(n.getCreatedAt())
                .sentAt(n.getSentAt())
                .build();
    }
}