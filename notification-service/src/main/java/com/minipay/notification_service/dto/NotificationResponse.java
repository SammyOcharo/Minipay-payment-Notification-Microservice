package com.minipay.notification_service.dto;

import com.minipay.notification_service.enums.NotificationStatus;
import com.minipay.notification_service.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private String id;
    private String paymentId;
    private String phoneNumber;
    private String message;
    private NotificationType type;
    private NotificationStatus status;
    private String providerMessageId;
    private String failureReason;
    private int retryCount;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}