package com.minipay.payment_service.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minipay.payment_service.controller.PaymentController;
import com.minipay.payment_service.dto.PaymentRequest;
import com.minipay.payment_service.enums.PaymentMethod;
import com.minipay.payment_service.security.JwtAuthenticationFilter;
import com.minipay.payment_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;


    private static final String IDEMPOTENCY_KEY = "idem-key-123";

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private PaymentRequest validRequest() {
        PaymentRequest r = new PaymentRequest();
        r.setAmount(new BigDecimal("500.00"));
        r.setCurrency("KES");
        r.setPhoneNumber("+254712345678");
        r.setPaymentMethod(PaymentMethod.MPESA);
        return r;
    }

    // ─── handleValidation ─────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void validation_nullAmount_returns400WithFieldError() throws Exception {
        PaymentRequest r = validRequest();
        r.setAmount(null);

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.amount").value("Amount is required"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @WithMockUser
    void validation_blankCurrency_returns400WithFieldError() throws Exception {
        PaymentRequest r = validRequest();
        r.setCurrency("");

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.currency").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @WithMockUser
    void validation_multipleInvalidFields_returnsAllFieldErrors() throws Exception {
        PaymentRequest r = new PaymentRequest(); // all nulls

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isMap())
                // all four @NotNull/@NotBlank fields must appear
                .andExpect(jsonPath("$.details.amount").value("Amount is required"))
                .andExpect(jsonPath("$.details.currency").value("Currency is required"))
                .andExpect(jsonPath("$.details.phoneNumber").value("Phone number is required"))
                .andExpect(jsonPath("$.details.paymentMethod").value("Payment method is required"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @WithMockUser
    void validation_amountBelowMin_returnsCorrectMessage() throws Exception {
        PaymentRequest r = validRequest();
        r.setAmount(new BigDecimal("0.50"));

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").value("Minimum amount is 1.00"));
    }

    @Test
    @WithMockUser
    void validation_invalidPhoneFormat_returnsCorrectMessage() throws Exception {
        PaymentRequest r = validRequest();
        r.setPhoneNumber("0712345678");

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.phoneNumber")
                        .value("Phone number must be valid e.g +254712345678"));
    }

    @Test
    @WithMockUser
    void validation_invalidCurrencyPattern_returnsCorrectMessage() throws Exception {
        PaymentRequest r = validRequest();
        r.setCurrency("kes"); // lowercase fails ^[A-Z]{3}$

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.currency")
                        .value("Currency must be a 3-letter code e.g KES, USD"));
    }

    @Test
    @WithMockUser
    void validation_responseHasNoExtraTopLevelKeys() throws Exception {
        PaymentRequest r = validRequest();
        r.setAmount(null);

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isBadRequest())
                // only timestamp, message, details — no "status", "path", etc.
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.path").doesNotExist());
    }

    // ─── handleResponseStatus ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    void responseStatusException_404_returnsCorrectStructure() throws Exception {
        when(paymentService.initiatePayment(any(), any(), any()))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found"));

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment not found"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @WithMockUser
    void responseStatusException_409_returnsCorrectStatus() throws Exception {
        when(paymentService.initiatePayment(any(), any(), any()))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.CONFLICT, "Duplicate idempotency key"));

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Duplicate idempotency key"));
    }

    @Test
    @WithMockUser
    void responseStatusException_403_returnsCorrectStatus() throws Exception {
        when(paymentService.initiatePayment(any(), any(), any()))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @WithMockUser
    void responseStatusException_nullReason_returnsNullMessage() throws Exception {
        // ResponseStatusException with no reason string
        when(paymentService.initiatePayment(any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                // message key exists but value is null — buildError("null", null)
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // ─── handleGeneral ────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void unexpectedException_returns500WithGenericMessage() throws Exception {
        when(paymentService.initiatePayment(any(), any(), any()))
                .thenThrow(new RuntimeException("Database exploded"));

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("An unexpected error occurred"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                // internal error detail must NOT leak to client
                .andExpect(jsonPath("$.details").doesNotExist());
    }

    @Test
    @WithMockUser
    void unexpectedException_internalDetailNotLeaked() throws Exception {
        when(paymentService.initiatePayment(any(), any(), any()))
                .thenThrow(new RuntimeException("SELECT * FROM users — leaked SQL"));

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("An unexpected error occurred"))
                // raw exception message must never appear in response body
                .andExpect(jsonPath("$").value(
                        not(hasToString(containsString("SELECT")))));
    }

    @Test
    @WithMockUser
    void nullPointerException_treatedAsGeneral500() throws Exception {
        when(paymentService.initiatePayment(any(), any(), any()))
                .thenThrow(new NullPointerException());

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message")
                        .value("An unexpected error occurred"));
    }

    // ─── buildError shape ─────────────────────────────────────────────────────

    @Test
    @WithMockUser
    void errorResponse_timestampIsIso8601Format() throws Exception {
        when(paymentService.initiatePayment(any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isInternalServerError())
                // LocalDateTime.now().toString() → "2026-06-24T11:07:55.123"
                .andExpect(jsonPath("$.timestamp",
                        matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")));
    }

    @Test
    @WithMockUser
    void validationError_detailsIsMapNotArray() throws Exception {
        PaymentRequest r = validRequest();
        r.setAmount(null);

        mockMvc.perform(post("/api/payments")
                        .with(csrf())
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(r)))
                .andExpect(status().isBadRequest())
                // details must be a flat key→message map, not an array
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.details[0]").doesNotExist());
    }
}