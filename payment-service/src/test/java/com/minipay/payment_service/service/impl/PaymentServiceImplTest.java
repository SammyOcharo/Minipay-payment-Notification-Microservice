package com.minipay.payment_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minipay.payment_service.dto.*;
import com.minipay.payment_service.enums.PaymentMethod;
import com.minipay.payment_service.enums.PaymentStatus;
import com.minipay.payment_service.gateway.PaymentGateway;
import com.minipay.payment_service.kafka.PaymentEventProducer;
import com.minipay.payment_service.mapper.PaymentMapper;
import com.minipay.payment_service.model.IdempotencyRecord;
import com.minipay.payment_service.model.Payment;
import com.minipay.payment_service.repository.PaymentRepository;
import com.minipay.payment_service.service.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Service Tests")
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private PaymentEventProducer eventProducer;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PaymentGateway mpesaGateway;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentRequest paymentRequest;
    private Payment payment;
    private PaymentResponse paymentResponse;
    private GatewayResponse successGatewayResponse;
    private GatewayResponse failedGatewayResponse;

    @BeforeEach
    void setUp() {
        paymentRequest = new PaymentRequest();
        paymentRequest.setAmount(new BigDecimal("100.00"));
        paymentRequest.setCurrency("KES");
        paymentRequest.setPhoneNumber("+254712345678");
        paymentRequest.setPaymentMethod(PaymentMethod.MPESA);
        paymentRequest.setDescription("Test payment");

        payment = Payment.builder()
                .id("payment-123")
                .idempotencyKey("idem-key-123")
                .amount(new BigDecimal("100.00"))
                .currency("KES")
                .phoneNumber("+254712345678")
                .paymentMethod(PaymentMethod.MPESA)
                .userEmail("test@minipay.com")
                .status(PaymentStatus.PENDING)
                .build();

        paymentResponse = new PaymentResponse();

        paymentResponse.setId("payment-123");
        paymentResponse.setIdempotencyKey("idem-key-123");
        paymentResponse.setAmount(new BigDecimal("100.00"));
        paymentResponse.setCurrency("KES");
        paymentResponse.setStatus(PaymentStatus.SUCCESS);

        successGatewayResponse = GatewayResponse.builder()
                .success(true)
                .gatewayReference("ws_CO_TEST123")
                .gatewayStatus("SUCCESS")
                .mpesaReceiptNumber("OEI2AK4RI0")
                .build();

        failedGatewayResponse = GatewayResponse.builder()
                .success(false)
                .gatewayStatus("FAILED")
                .failureReason("Insufficient funds")
                .build();

        // Set up gateway list
        List<PaymentGateway> gateways = List.of(mpesaGateway);
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                idempotencyService,
                eventProducer,
                paymentMapper,
                objectMapper,
                gateways
        );
    }

    // ─── Initiate Payment Tests ───────────────────────────

    @Test
    @DisplayName("Should successfully initiate MPESA payment")
    void shouldSuccessfullyInitiateMpesaPayment() {
        // Arrange
        when(idempotencyService.findExistingRecord("idem-key-123"))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(payment);
        when(mpesaGateway.supports(PaymentMethod.MPESA))
                .thenReturn(true);
        when(mpesaGateway.processPayment(any(Payment.class)))
                .thenReturn(successGatewayResponse);
        when(paymentMapper.toResponse(any(Payment.class)))
                .thenReturn(paymentResponse);
        when(idempotencyService.hashRequest(any()))
                .thenReturn("hash-123");

        // Act
        PaymentResponse result = paymentService.initiatePayment(
                "idem-key-123", paymentRequest, "test@minipay.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("payment-123");
        verify(paymentRepository, times(3)).save(any(Payment.class));
        verify(mpesaGateway).processPayment(any(Payment.class));
        verify(idempotencyService).saveRecord(
                anyString(), anyString(), any(), anyInt(), anyString());
    }

    @Test
    @DisplayName("Should return cached response for duplicate idempotency key")
    void shouldReturnCachedResponseForDuplicateIdempotencyKey()
            throws Exception {
        // Arrange
        IdempotencyRecord existingRecord = IdempotencyRecord.builder()
                .idempotencyKey("idem-key-123")
                .responseBody("{\"id\":\"payment-123\"}")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(idempotencyService.findExistingRecord("idem-key-123"))
                .thenReturn(Optional.of(existingRecord));
        when(objectMapper.readValue(anyString(), eq(PaymentResponse.class)))
                .thenReturn(paymentResponse);

        // Act
        PaymentResponse result = paymentService.initiatePayment(
                "idem-key-123", paymentRequest, "test@minipay.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("payment-123");
        // Gateway should NOT be called for duplicate request
        verify(mpesaGateway, never()).processPayment(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle failed payment gateway response")
    void shouldHandleFailedPaymentGatewayResponse() {
        // Arrange
        when(idempotencyService.findExistingRecord(anyString()))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(payment);
        when(mpesaGateway.supports(PaymentMethod.MPESA))
                .thenReturn(true);
        when(mpesaGateway.processPayment(any(Payment.class)))
                .thenReturn(failedGatewayResponse);

        PaymentResponse failedResponse =  new PaymentResponse();
        failedResponse.setId("payment-123");
        failedResponse.setStatus(PaymentStatus.FAILED);
        failedResponse.setFailureReason("Insufficient funds");
        when(paymentMapper.toResponse(any(Payment.class)))
                .thenReturn(failedResponse);
        when(idempotencyService.hashRequest(any()))
                .thenReturn("hash-123");

        // Act
        PaymentResponse result = paymentService.initiatePayment(
                "idem-key-456", paymentRequest, "test@minipay.com");

        // Assert
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getFailureReason())
                .isEqualTo("Insufficient funds");
    }

    @Test
    @DisplayName("Should throw exception for unsupported payment method")
    void shouldThrowExceptionForUnsupportedPaymentMethod() {
        // Arrange
        when(idempotencyService.findExistingRecord(anyString()))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class)))
                .thenReturn(payment);
        when(mpesaGateway.supports(any()))
                .thenReturn(false);

        paymentRequest.setPaymentMethod(PaymentMethod.CARD);

        // Act & Assert
        assertThatThrownBy(() -> paymentService.initiatePayment(
                "idem-key-789", paymentRequest, "test@minipay.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unsupported payment method");
    }

    // ─── Get Payment Tests ────────────────────────────────

    @Test
    @DisplayName("Should return payment by ID for correct user")
    void shouldReturnPaymentByIdForCorrectUser() {
        // Arrange
        payment.setUserEmail("test@minipay.com");
        when(paymentRepository.findById("payment-123"))
                .thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment))
                .thenReturn(paymentResponse);

        // Act
        PaymentResponse result = paymentService.getPaymentById(
                "payment-123", "test@minipay.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("payment-123");
    }

    @Test
    @DisplayName("Should throw 404 for non-existent payment")
    void shouldThrow404ForNonExistentPayment() {
        // Arrange
        when(paymentRepository.findById("non-existent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> paymentService.getPaymentById(
                "non-existent", "test@minipay.com"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e)
                        .getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should throw 403 when user tries to access another user payment")
    void shouldThrow403WhenAccessingAnotherUsersPayment() {
        // Arrange
        payment.setUserEmail("other@minipay.com");
        when(paymentRepository.findById("payment-123"))
                .thenReturn(Optional.of(payment));

        // Act & Assert
        assertThatThrownBy(() -> paymentService.getPaymentById(
                "payment-123", "test@minipay.com"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e)
                        .getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ─── Payment History Tests ────────────────────────────

    @Test
    @DisplayName("Should return paginated payment history for user")
    void shouldReturnPaginatedPaymentHistoryForUser() {
        // Arrange
        Page<Payment> paymentPage = new PageImpl<>(List.of(payment));
        when(paymentRepository.findByUserEmail(
                eq("test@minipay.com"), any(PageRequest.class)))
                .thenReturn(paymentPage);
        when(paymentMapper.toResponse(payment))
                .thenReturn(paymentResponse);

        // Act
        Page<PaymentResponse> result = paymentService.getPaymentHistory(
                "test@minipay.com", PageRequest.of(0, 10));

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId())
                .isEqualTo("payment-123");
    }
}