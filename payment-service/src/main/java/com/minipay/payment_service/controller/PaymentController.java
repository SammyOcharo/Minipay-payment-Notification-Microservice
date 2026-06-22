package com.minipay.payment_service.controller;

import com.minipay.payment_service.dto.PaymentRequest;
import com.minipay.payment_service.dto.PaymentResponse;
import com.minipay.payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Initiate a new payment")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal String email) {

        log.info("Payment request received. User: {} Method: {}",
                email, request.getPaymentMethod());

        PaymentResponse response = paymentService.initiatePayment(
                idempotencyKey,
                request,
                email
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable String id,
            @AuthenticationPrincipal String email) {

        PaymentResponse response = paymentService.getPaymentById(
                id, email);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get payment history")
    public ResponseEntity<Page<PaymentResponse>> getPaymentHistory(
            @AuthenticationPrincipal String email,
            @PageableDefault(size = 10, sort = "createdAt")
            Pageable pageable) {

        Page<PaymentResponse> history = paymentService
                .getPaymentHistory(email, pageable);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/mpesa/callback")
    @Operation(summary = "M-PESA payment callback webhook")
    public ResponseEntity<Void> mpesaCallback(
            @RequestBody String callbackBody) {

        log.info("M-PESA callback received");
        paymentService.handleMpesaCallback(callbackBody);
        return ResponseEntity.ok().build();
    }
}