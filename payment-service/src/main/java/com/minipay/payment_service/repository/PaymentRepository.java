package com.minipay.payment_service.repository;

import com.minipay.payment_service.enums.PaymentStatus;
import com.minipay.payment_service.model.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Page<Payment> findByUserEmail(String userEmail, Pageable pageable);

    Page<Payment> findByUserEmailAndStatus(
            String userEmail,
            PaymentStatus status,
            Pageable pageable
    );

    boolean existsByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByGatewayReference(String checkoutRequestId);
}