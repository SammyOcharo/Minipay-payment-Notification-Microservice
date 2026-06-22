package com.minipay.payment_service.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.minipay.payment_service.dto.GatewayResponse;
import com.minipay.payment_service.dto.PaymentEvent;
import com.minipay.payment_service.dto.PaymentRequest;
import com.minipay.payment_service.dto.PaymentResponse;
import com.minipay.payment_service.enums.PaymentStatus;
import com.minipay.payment_service.gateway.PaymentGateway;
import com.minipay.payment_service.kafka.PaymentEventProducer;
import com.minipay.payment_service.mapper.PaymentMapper;
import com.minipay.payment_service.model.IdempotencyRecord;
import com.minipay.payment_service.model.Payment;
import com.minipay.payment_service.repository.PaymentRepository;
import com.minipay.payment_service.service.IdempotencyService;
import com.minipay.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;
    private final PaymentEventProducer eventProducer;
    private final PaymentMapper paymentMapper;
    private final ObjectMapper objectMapper;
    private final List<PaymentGateway> paymentGateways;

    @Override
    @Transactional
    public PaymentResponse initiatePayment(
            String idempotencyKey,
            PaymentRequest request,
            String userEmail) {

        log.info("Initiating payment. IdempotencyKey: {} User: {}",
                idempotencyKey, userEmail);

        // ─── Step 1: Check Idempotency ────────────────────
        Optional<IdempotencyRecord> existingRecord =
                idempotencyService.findExistingRecord(idempotencyKey);

        if (existingRecord.isPresent()) {
            log.info("Duplicate request detected. " +
                    "Returning cached response for key: {}", idempotencyKey);
            try {
                return objectMapper.readValue(
                        existingRecord.get().getResponseBody(),
                        PaymentResponse.class
                );
            } catch (Exception e) {
                log.error("Failed to deserialize cached response", e);
            }
        }

        // ─── Step 2: Create Payment Record ────────────────
        Payment payment = Payment.builder()
                .idempotencyKey(idempotencyKey)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .phoneNumber(request.getPhoneNumber())
                .paymentMethod(request.getPaymentMethod())
                .userEmail(userEmail)
                .description(request.getDescription())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment record created: {}", payment.getId());

        // ─── Step 3: Process via Gateway ──────────────────
        payment.setStatus(PaymentStatus.PROCESSING);
        payment = paymentRepository.save(payment);

        PaymentGateway gateway = resolveGateway(request.getPaymentMethod());
        GatewayResponse gatewayResponse = gateway.processPayment(payment);

        // ─── Step 4: Update Payment with Gateway Response ─
        updatePaymentFromGatewayResponse(payment, gatewayResponse);
        payment = paymentRepository.save(payment);

        // ─── Step 5: Publish Kafka Event ──────────────────
        publishPaymentEventAsync(payment);

        // ─── Step 6: Build Response ───────────────────────
        PaymentResponse response = paymentMapper.toResponse(payment);

        // ─── Step 7: Save Idempotency Record ─────────────
        idempotencyService.saveRecord(
                idempotencyKey,
                idempotencyService.hashRequest(request),
                response,
                HttpStatus.CREATED.value(),
                payment.getId()
        );

        log.info("Payment completed. Id: {} Status: {}",
                payment.getId(), payment.getStatus());

        return response;
    }

    @Override
    public PaymentResponse getPaymentById(String id, String userEmail) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Payment not found: " + id
                ));

        if (!payment.getUserEmail().equals(userEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied to payment: " + id
            );
        }

        return paymentMapper.toResponse(payment);
    }

    @Override
    public Page<PaymentResponse> getPaymentHistory(
            String userEmail,
            Pageable pageable) {
        return paymentRepository
                .findByUserEmail(userEmail, pageable)
                .map(paymentMapper::toResponse);
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public PaymentResponse handleMpesaCallback(String body) {
        log.info("M-PESA callback received: {}", body);
        try {
            Map<String, Object> callbackData = objectMapper
                    .readValue(body, Map.class);
            Map<String, Object> stkCallback = (Map<String, Object>)
                    ((Map<String, Object>) callbackData.get("Body"))
                            .get("stkCallback");

            String merchantRequestId = (String) stkCallback
                    .get("MerchantRequestID");
            String checkoutRequestId = (String) stkCallback
                    .get("CheckoutRequestID");
            int resultCode = ((Number) stkCallback
                    .get("ResultCode")).intValue();

            Payment payment = paymentRepository
                    .findByGatewayReference(checkoutRequestId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Payment not found for checkout: "
                                    + checkoutRequestId));

            if (resultCode == 0) {
                List<Map<String, Object>> items = (List<Map<String, Object>>)
                        ((Map<String, Object>) stkCallback
                                .get("CallbackMetadata")).get("Item");

                String receiptNumber = items.stream()
                        .filter(i -> "MpesaReceiptNumber"
                                .equals(i.get("Name")))
                        .map(i -> String.valueOf(i.get("Value")))
                        .findFirst().orElse("UNKNOWN");

                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setMpesaReceiptNumber(receiptNumber);
                payment.setCompletedAt(LocalDateTime.now());
                log.info("M-PESA payment SUCCESS. Receipt: {}",
                        receiptNumber);
            } else {
                String resultDesc = (String) stkCallback.get("ResultDesc");
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(resultDesc);
                log.warn("M-PESA payment FAILED: {}", resultDesc);
            }

            payment = paymentRepository.save(payment);
            publishPaymentEventAsync(payment);
            return paymentMapper.toResponse(payment);

        } catch (Exception e) {
            log.error("Error processing M-PESA callback: {}",
                    e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Callback processing failed");
        }
    }

    // ─── Private Helpers ──────────────────────────────────

    private PaymentGateway resolveGateway(
            com.minipay.payment_service.enums.PaymentMethod method) {
        return paymentGateways.stream()
                .filter(g -> g.supports(method))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported payment method: " + method
                ));
    }

    private void updatePaymentFromGatewayResponse(
            Payment payment,
            GatewayResponse response) {
        if (response.isSuccess()) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setCompletedAt(LocalDateTime.now());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(response.getFailureReason());
        }
        payment.setGatewayReference(response.getGatewayReference());
        payment.setGatewayStatus(response.getGatewayStatus());
        payment.setMpesaReceiptNumber(response.getMpesaReceiptNumber());
        payment.setMerchantRequestId(response.getMerchantRequestId());
    }

    @Async
    protected void publishPaymentEventAsync(Payment payment) {
        try {
            PaymentEvent event = paymentMapper.toEvent(payment);
            event.setTimestamp(LocalDateTime.now());
            log.info("Publishing event for paymentId: {} status: {}",
                    event.getPaymentId(), event.getStatus());
            eventProducer.publishPaymentEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish payment event for: {}",
                    payment.getId(), e);
        }
    }
}