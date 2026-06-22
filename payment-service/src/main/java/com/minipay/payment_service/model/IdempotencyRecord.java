package com.minipay.payment_service.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private String requestHash;         // Hash of request body

    @Column(nullable = false, columnDefinition = "TEXT")
    private String responseBody;        // Cached response JSON

    @Column(nullable = false)
    private Integer responseStatus;     // HTTP status code

    @Column(nullable = false)
    private String paymentId;           // Reference to payment

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Records expire after 24 hours
    private LocalDateTime expiresAt;
}
