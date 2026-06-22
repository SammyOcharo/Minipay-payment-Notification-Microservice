package com.minipay.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventDto {

    private String paymentId;
    private String idempotencyKey;
    private String userEmail;
    private String phoneNumber;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String status;
    private String gatewayReference;
    private String mpesaReceiptNumber;
    private String failureReason;
    private LocalDateTime timestamp;
}