package com.minipay.payment_service.dto;

import com.minipay.payment_service.enums.PaymentMethod;
import com.minipay.payment_service.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {

    private String id;
    private String idempotencyKey;
    private BigDecimal amount;
    private String currency;
    private String phoneNumber;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String gatewayReference;
    private String mpesaReceiptNumber;
    private String failureReason;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
