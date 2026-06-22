package com.minipay.payment_service.dto;


import com.minipay.payment_service.enums.PaymentMethod;
import com.minipay.payment_service.enums.PaymentStatus;
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
public class PaymentEvent {

    private String paymentId;
    private String idempotencyKey;
    private String userEmail;
    private String phoneNumber;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String gatewayReference;
    private String mpesaReceiptNumber;
    private String failureReason;
    private LocalDateTime timestamp;
}
