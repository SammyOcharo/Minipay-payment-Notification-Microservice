package com.minipay.notification_service.service;

import com.minipay.notification_service.dto.NotificationResponse;
import com.minipay.notification_service.dto.PaymentEventDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    void processPaymentEvent(PaymentEventDto event);
    NotificationResponse getNotificationByPaymentId(String paymentId);
    Page<NotificationResponse> getNotificationsByEmail(
            String email, Pageable pageable);
}