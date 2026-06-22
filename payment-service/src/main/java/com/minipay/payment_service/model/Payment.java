package com.minipay.payment_service.model;


import com.minipay.payment_service.enums.PaymentMethod;
import com.minipay.payment_service.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_idempotency_key",
                columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_phone_number",
                columnList = "phoneNumber"),
        @Index(name = "idx_status",
                columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // ─── Idempotency ──────────────────────────────────────
    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    // ─── Payment Details ──────────────────────────────────
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    // ─── User Info ────────────────────────────────────────
    @Column(nullable = false)
    private String userEmail;

    // ─── Gateway Response ─────────────────────────────────
    private String gatewayReference;      // M-PESA checkout request ID etc
    private String gatewayStatus;         // Raw status from gateway
    private String failureReason;         // Why it failed if it did

    // ─── M-PESA Specific ──────────────────────────────────
    private String mpesaReceiptNumber;    // M-PESA receipt after success
    private String merchantRequestId;

    // ─── Metadata ─────────────────────────────────────────
    private String description;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;
}