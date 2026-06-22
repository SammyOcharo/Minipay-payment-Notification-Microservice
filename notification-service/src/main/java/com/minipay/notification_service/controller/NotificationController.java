package com.minipay.notification_service.controller;

import com.minipay.notification_service.dto.NotificationResponse;
import com.minipay.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<NotificationResponse> getByPaymentId(
            @PathVariable String paymentId) {
        return ResponseEntity.ok(
                notificationService
                        .getNotificationByPaymentId(paymentId));
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<Page<NotificationResponse>> getByEmail(
            @PathVariable String email,
            @PageableDefault(size = 10, sort = "createdAt")
            Pageable pageable) {
        return ResponseEntity.ok(
                notificationService
                        .getNotificationsByEmail(email, pageable));
    }
}