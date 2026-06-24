package com.minipay.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minipay.payment_service.dto.PaymentRequest;
import com.minipay.payment_service.dto.PaymentResponse;
import com.minipay.payment_service.enums.PaymentMethod;
import com.minipay.payment_service.enums.PaymentStatus;
import com.minipay.payment_service.security.JwtAuthenticationFilter;
import com.minipay.payment_service.service.PaymentService;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ─── Fixtures ─────────────────────────────────────────────────────────────

    private static final String USER_EMAIL      = "sam@minipay.com";
    private static final String IDEMPOTENCY_KEY = "idem-key-123";

    private PaymentRequest validRequest() {
        PaymentRequest r = new PaymentRequest();
        r.setAmount(new BigDecimal("500.00"));
        r.setCurrency("KES");
        r.setPhoneNumber("+254712345678");
        r.setPaymentMethod(PaymentMethod.MPESA);
        r.setDescription("Test payment");
        return r;
    }

    private PaymentResponse sampleResponse() {
        PaymentResponse r = new PaymentResponse();
        r.setId("pay-001");
        r.setIdempotencyKey(IDEMPOTENCY_KEY);
        r.setAmount(new BigDecimal("500.00"));
        r.setCurrency("KES");
        r.setPhoneNumber("+254712345678");
        r.setPaymentMethod(PaymentMethod.MPESA);
        r.setStatus(PaymentStatus.PENDING);
        r.setDescription("Test payment");
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    // ─── POST /api/payments ───────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_validRequest_returns201WithBody() throws Exception {
        // Use any() for email — @WithMockUser principal is UserDetails, not String,
        // so @AuthenticationPrincipal String resolves to null in MockMvc tests
        when(paymentService.initiatePayment(
                eq(IDEMPOTENCY_KEY), any(PaymentRequest.class), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("pay-001"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").value("MPESA"))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.currency").value("KES"))
                .andExpect(jsonPath("$.idempotencyKey").value(IDEMPOTENCY_KEY));
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_serviceThrows_returns500() throws Exception {
        when(paymentService.initiatePayment(any(), any(), any()))
                .thenThrow(new RuntimeException("Upstream error"));

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isInternalServerError());
    }

    // ─── @Valid constraint tests ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_nullAmount_returns400() throws Exception {
        PaymentRequest r = validRequest();
        r.setAmount(null);
        assertValidationFails(r);
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_amountBelowMinimum_returns400() throws Exception {
        PaymentRequest r = validRequest();
        r.setAmount(new BigDecimal("0.50"));
        assertValidationFails(r);
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_amountAboveMaximum_returns400() throws Exception {
        PaymentRequest r = validRequest();
        r.setAmount(new BigDecimal("150001.00"));
        assertValidationFails(r);
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_amountAtMinBoundary_passes() throws Exception {
        PaymentRequest r = validRequest();
        r.setAmount(new BigDecimal("1.00"));
        when(paymentService.initiatePayment(any(), any(), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_amountAtMaxBoundary_passes() throws Exception {
        PaymentRequest r = validRequest();
        r.setAmount(new BigDecimal("150000.00"));
        when(paymentService.initiatePayment(any(), any(), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_blankCurrency_returns400() throws Exception {
        PaymentRequest r = validRequest();
        r.setCurrency("");
        assertValidationFails(r);
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_invalidCurrencyFormat_returns400() throws Exception {
        PaymentRequest r = validRequest();
        r.setCurrency("ke");
        assertValidationFails(r);
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_lowercaseCurrency_returns400() throws Exception {
        PaymentRequest r = validRequest();
        r.setCurrency("kes");
        assertValidationFails(r);
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_blankPhoneNumber_returns400() throws Exception {
        PaymentRequest r = validRequest();
        r.setPhoneNumber("");
        assertValidationFails(r);
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_invalidPhoneNumber_returns400() throws Exception {
        PaymentRequest r = validRequest();
        r.setPhoneNumber("0712345678");
        assertValidationFails(r);
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_phoneWithoutPlus_passes() throws Exception {
        PaymentRequest r = validRequest();
        r.setPhoneNumber("254712345678");
        when(paymentService.initiatePayment(any(), any(), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_nullPaymentMethod_returns400() throws Exception {
        String body = """
                {
                  "amount": 500.00,
                  "currency": "KES",
                  "phoneNumber": "+254712345678",
                  "paymentMethod": null
                }
                """;

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void initiatePayment_descriptionIsOptional_passes() throws Exception {
        PaymentRequest r = validRequest();
        r.setDescription(null);
        when(paymentService.initiatePayment(any(), any(), any()))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isCreated());
    }


    @Test
    void mpesaCallback_serviceThrows_returns500() throws Exception {
        doThrow(new RuntimeException("Kafka unavailable"))
                .when(paymentService).handleMpesaCallback(anyString());

        mockMvc.perform(post("/api/payments/mpesa/callback")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isInternalServerError());
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private void assertValidationFails(PaymentRequest request) throws Exception {
        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }
}