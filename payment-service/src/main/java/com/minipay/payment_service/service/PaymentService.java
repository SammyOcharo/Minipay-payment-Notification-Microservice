package com.minipay.payment_service.service;

import com.minipay.payment_service.dto.PaymentRequest;
import com.minipay.payment_service.dto.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    PaymentResponse initiatePayment(
            String idempotencyKey,
            PaymentRequest request,
            String userEmail
    );

    PaymentResponse getPaymentById(String id, String userEmail);

    Page<PaymentResponse> getPaymentHistory(
            String userEmail,
            Pageable pageable
    );

    PaymentResponse handleMpesaCallback(String body);
}