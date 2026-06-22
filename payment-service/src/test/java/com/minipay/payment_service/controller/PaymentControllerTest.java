//package com.minipay.payment_service.controller;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.minipay.payment_service.dto.PaymentRequest;
//import com.minipay.payment_service.dto.PaymentResponse;
//import com.minipay.payment_service.enums.PaymentMethod;
//import com.minipay.payment_service.enums.PaymentStatus;
//import com.minipay.payment_service.service.PaymentService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.data.domain.*;
//import org.springframework.http.MediaType;
//import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.math.BigDecimal;
//import java.util.List;
//
//import static org.hamcrest.Matchers.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.*;
//import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
//@WebMvcTest(PaymentController.class)
//@DisplayName("PaymentController")
//class PaymentControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private PaymentService paymentService;
//
//    // ---------------------------------------------------------------
//    // Fixtures
//    // ---------------------------------------------------------------
//
//    private static final String BASE_URL        = "/api/payments";
//    private static final String IDEMPOTENCY_KEY = "idem-key-abc-123";
//    private static final String USER_EMAIL      = "user@example.com";
//    private static final String PAYMENT_ID      = "pay_001";
//
//    private PaymentRequest validRequest;
//    private PaymentResponse paymentResponse;
//
//    @BeforeEach
//    void setUp() {
//        validRequest = new PaymentRequest();
//        validRequest.setPaymentMethod(PaymentMethod.MPESA);
//        validRequest.setAmount(BigDecimal.valueOf(500.00));
//        // set any other @Valid-required fields here
//
//        paymentResponse = new PaymentResponse();
//        paymentResponse.setId(PAYMENT_ID);
//        paymentResponse.setStatus(PaymentStatus.PENDING);
//        paymentResponse.setPaymentMethod(PaymentMethod.MPESA);
//    }
//
//    // ================================================================
//    // POST /api/payments — initiatePayment
//    // ================================================================
//
//    @Nested
//    @DisplayName("POST /api/payments — initiatePayment")
//    class InitiatePayment {
//
//        @Test
//        @DisplayName("returns 201 CREATED with PaymentResponse on valid request")
//        void initiatePayment_validRequest_returns201() throws Exception {
//            when(paymentService.initiatePayment(IDEMPOTENCY_KEY, validRequest, USER_EMAIL))
//                    .thenReturn(paymentResponse);
//
//            mockMvc.perform(post(BASE_URL)
//                            .with(csrf())
//                            .with(SecurityMockMvcRequestPostProcessors.user(USER_EMAIL))
//                            .header("Idempotency-Key", IDEMPOTENCY_KEY)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(validRequest)))
//                    .andExpect(status().isCreated())
//                    .andExpect(jsonPath("$.id").value(PAYMENT_ID))
//                    .andExpect(jsonPath("$.status").value("PENDING"))
//                    .andExpect(jsonPath("$.paymentMethod").value("MPESA"));
//
//            verify(paymentService).initiatePayment(IDEMPOTENCY_KEY, validRequest, USER_EMAIL);
//        }
//
//        @Test
//        @DisplayName("passes idempotency key, request body and principal email to service")
//        void initiatePayment_delegatesToServiceWithCorrectArgs() throws Exception {
//            when(paymentService.initiatePayment(any(), any(), any()))
//                    .thenReturn(paymentResponse);
//
//            mockMvc.perform(post(BASE_URL)
//                            .with(csrf())
//                            .with(SecurityMockMvcRequestPostProcessors.user(USER_EMAIL))
//                            .header("Idempotency-Key", IDEMPOTENCY_KEY)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(validRequest)))
//                    .andExpect(status().isCreated());
//
//            verify(paymentService).initiatePayment(
//                    eq(IDEMPOTENCY_KEY),
//                    any(PaymentRequest.class),
//                    eq(USER_EMAIL));
//        }
//
//        @Test
//        @DisplayName("returns 400 when Idempotency-Key header is missing")
//        void initiatePayment_missingIdempotencyKey_returns400() throws Exception {
//            mockMvc.perform(post(BASE_URL)
//                            .with(csrf())
//                            .with(SecurityMockMvcRequestPostProcessors.user(USER_EMAIL))
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(validRequest)))
//                    .andExpect(status().isBadRequest());
//
//            verifyNoInteractions(paymentService);
//        }
//
//        @Test
//        @DisplayName("returns 400 when request body is missing")
//        void initiatePayment_missingBody_returns400() throws Exception {
//            mockMvc.perform(post(BASE_URL)
//                            .with(csrf())
//                            .with(SecurityMockMvcRequestPostProcessors.user(USER_EMAIL))
//                            .header("Idempotency-Key", IDEMPOTENCY_KEY)
//                            .contentType(MediaType.APPLICATION_JSON))
//                    .andExpect(status().isBadRequest());
//
//            verifyNoInteractions(paymentService);
//        }
//
//        @Test
//        @DisplayName("returns 400 when @Valid constraints are violated")
//        void initiatePayment_invalidBody_returns400() throws Exception {
//            PaymentRequest invalid = new PaymentRequest(); // missing required fields
//
//            mockMvc.perform(post(BASE_URL)
//                            .with(csrf())
//                            .with(SecurityMockMvcRequestPostProcessors.user(USER_EMAIL))
//                            .header("Idempotency-Key", IDEMPOTENCY_KEY)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(invalid)))
//                    .andExpect(status().isBadRequest());
//
//            verifyNoInteractions(paymentService);
//        }
//
//        @Test
//        @DisplayName("returns 401 when request is unauthenticated")
//        void initiatePayment_unauthenticated_returns401() throws Exception {
//            mockMvc.perform(post(BASE_URL)
//                            .with(csrf())
//                            .header("Idempotency-Key", IDEMPOTENCY_KEY)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(validRequest)))
//                    .andExpect(status().isUnauthorized());
//
//            verifyNoInteractions(paymentService);
//        }
//
//        @Test
//        @DisplayName("propagates service exception to the caller")
//        void initiatePayment_serviceThrows_propagatesException() throws Exception {
//            when(paymentService.initiatePayment(any(), any(), any()))
//                    .thenThrow(new RuntimeException("Payment gateway error"));
//
//            mockMvc.perform(post(BASE_URL)
//                            .with(csrf())
//                            .with(SecurityMockMvcRequestPostProcessors.user(USER_EMAIL))
//                            .header("Idempotency-Key", IDEMPOTENCY_KEY)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(objectMapper.writeValueAsString(validRequest)))
//                    .andExpect(status().isInternalServerError());
//        }
//    }
//
//    // ================================================================
//    // GET /api/payments/{id} — getPayment
//    // ================================================================
//
//    @Nested
//    @DisplayName("GET /api/payments/{id} — getPayment")
//    class GetPayment {
//
//        @Test
//        @DisplayName("returns 200 OK with PaymentResponse for a valid ID")
//        void getPayment_validId_returns200() throws Exception {
//            when(paymentService.getPaymentById(PAYMENT_ID, USER_EMAIL))
//                    .thenReturn(paymentResponse);
//
//            mockMvc.perform(get(BASE_URL + "/{id}", PAYMENT_ID)
//                            .with(SecurityMockMvcRequestPostProcessors.user(USER_EMAIL)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.id").value(PAYMENT_ID))
//                    .andExpect(jsonPath("$.status").value("PENDING"))
//                    .andExpect(jsonPath("$.paymentMethod").value("MPESA"));
//
//            verify(paymentService).getPaymentById(PAYMENT_ID, USER_EMAIL);
//        }
//
//        @Test
//        @DisplayName("passes path variable and principal email to service")
//        void getPayment_delegatesCorrectArgsToService() throws Exception {
//            when(paymentService.getPaymentById(any(), any()))
//                    .thenReturn(paymentResponse);
//
//            mockMvc.perform(get(BASE_URL + "/{id}", PAYMENT_ID)
//                            .with(SecurityMockMvcRequestPostProcessors.user(USER_EMAIL)))
//                    .andExpect(status().isOk());
//
//            verify(paymentService).getPaymentById(eq(PAYMENT_ID), eq(USER_EMAIL));
//        }
//
//        @Test
//        @DisplayName("returns 401 when request is unauthenticated")
//        void getPayment_unauthenticated_returns401() throws Exception {
//            mockMvc.perform(get(BASE_URL + "/{id}", PAYMENT_ID))
//                    .andExpect(status().isUnauthorized());
//
//            verifyNoInteractions(paymentService);
//        }
//
//        @Test
//        @DisplayName("propagates service exception (e.g. 404) to the caller")
//        void getPayment_serviceThrows_propagatesException() throws Exception {
//            when(paymentService.getPaymentById(PAYMENT_ID, USER_EMAIL))
//                    .thenThrow(new RuntimeException("Payment not found"));
//
//            mockMvc.perform(get(BASE_URL + "/{id}", PAYMENT_ID)
//                            .with(SecurityMockMvcRequestPostProcessors.user(USER_EMAIL)))
//                    .andExpect(status().isInternalServerError());
//        }
//    }
//
//    // ================================================================
//    // GET /api/payments — getPaymentHistory
//    // ================================================================
//
//    @Nested
//    @DisplayName("GET /api/payments — getPaymentHistory")
//    class GetPaymentHistory {
//
//        @Test
//        @DisplayName("returns 200 OK with paged PaymentResponse list")
//        void getPaymentHistory_returns200WithPage() throws Exception {
//            Page<PaymentResponse> page = new PageImpl<>(
//                    List.of(paymentResponse),
//                    PageRequest.of(0, 10, Sort.by("createdAt")),
//                    1);
//
//            when(paymentService.getPaymentHistory(eq(USER_EMAIL), any(Pageable.class)))
//                    .thenReturn(page);
//
//            mockMvc.perform(get(BASE_URL)
//                            .with(SecurityMockMvcRequestPostProcessors.user(USER_EMAIL)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.content", hasSize(1)))
//                    .andExpect(jsonPath("$.content[0].id").value(PAYMENT_ID))
//                    .andExpect(jsonPath("$.totalElements").value(1));
//
//            verify(paymentService).getPaymentHistory(eq(USER_EMAIL), any(Pageable.class));
//        }
//
//        @Test
//        @DisplayName("returns 200 OK with empty page when history is empty")
//        void getPaymentHistory_emptyHistory_returnsEmptyPage() throws Exception {
//            Page<PaymentResponse> emptyPage = Page.empty();
//            when(paymentService.getPaymentHistory(eq(USER_EMAIL), any(Pageable.class)))
//                    .thenReturn(emptyPage);
//
//            mockMvc.perform(get(BASE_URL)
//                            .with(SecurityMockMvcRequestPostProcessors.user(USER_EMAIL)))
//                    .andExpect(status().isOk())
//                    .andExpect(jsonPath("$.content", hasSize(0)))
//                    .andExpect(jsonPath("$.totalElements").value(0));
//        }
//
//        @Test
//        @DisplayName("applies @PageableDefault size=10 when no pageable params provided")
//        void getPaymentHistory_appliesDefaultPageable() throws Exception {
//            when(paymentService.getPaymentHistory(eq(USER_EMAIL), any(Pageable.class)))
//                    .thenReturn(Page.empty());
//
//            mockMvc.perform(get(BASE_URL)
//                            .with(SecurityMockMvcRequestPostProcessors.user(USER_EMAIL)))
//                    .andExpect(status().isOk());
//
//            verify(paymentService).getPaymentHistory(
//                    eq(USER_EMAIL),
//                    argThat(p -> p.getPageSize() == 10));
//        }
//
//        @Test
//        @DisplayName("respects explicit page and size query parameters")
//        void getPaymentHistory_customPageableParams_passedToService() throws Exception {
//            when(paymentService.getPaymentHistory(eq(USER_EMAIL), any(Pageable.class)))
//                    .thenReturn(Page.empty());
//
//            mockMvc.perform(get(BASE_URL)
//                            .param("page", "2")
//                            .param("size", "5")
//                            .with(SecurityMockMvcRequestPostProcessors.user(USER_EMAIL)))
//                    .andExpect(status().isOk());
//
//            verify(paymentService).getPaymentHistory(
//                    eq(USER_EMAIL),
//                    argThat(p -> p.getPageNumber() == 2 && p.getPageSize() == 5));
//        }
//
//        @Test
//        @DisplayName("returns 401 when request is unauthenticated")
//        void getPaymentHistory_unauthenticated_returns401() throws Exception {
//            mockMvc.perform(get(BASE_URL))
//                    .andExpect(status().isUnauthorized());
//
//            verifyNoInteractions(paymentService);
//        }
//
//        @Test
//        @DisplayName("propagates service exception to the caller")
//        void getPaymentHistory_serviceThrows_propagatesException() throws Exception {
//            when(paymentService.getPaymentHistory(any(), any()))
//                    .thenThrow(new RuntimeException("DB error"));
//
//            mockMvc.perform(get(BASE_URL)
//                            .with(SecurityMockMvcRequestPostProcessors.user(USER_EMAIL)))
//                    .andExpect(status().isInternalServerError());
//        }
//    }
//
//    // ================================================================
//    // POST /api/payments/mpesa/callback — mpesaCallback
//    // ================================================================
//
//    @Nested
//    @DisplayName("POST /api/payments/mpesa/callback — mpesaCallback")
//    class MpesaCallback {
//
//        private static final String CALLBACK_BODY =
//                "{\"Body\":{\"stkCallback\":{\"ResultCode\":0,\"ResultDesc\":\"Success\"}}}";
//
//        @Test
//        @DisplayName("returns 200 OK and delegates callback body to service")
//        void mpesaCallback_validBody_returns200() throws Exception {
//            doNothing().when(paymentService).handleMpesaCallback(CALLBACK_BODY);
//
//            mockMvc.perform(post(BASE_URL + "/mpesa/callback")
//                            .with(csrf())
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(CALLBACK_BODY))
//                    .andExpect(status().isOk());
//
//            verify(paymentService).handleMpesaCallback(CALLBACK_BODY);
//        }
//
//        @Test
//        @DisplayName("response body is empty (Void return)")
//        void mpesaCallback_responseBodyIsEmpty() throws Exception {
//            doNothing().when(paymentService).handleMpesaCallback(any());
//
//            mockMvc.perform(post(BASE_URL + "/mpesa/callback")
//                            .with(csrf())
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(CALLBACK_BODY))
//                    .andExpect(status().isOk())
//                    .andExpect(content().string(emptyOrNullString()));
//        }
//
//        @Test
//        @DisplayName("accepts plain-text callback body")
//        void mpesaCallback_plainTextBody_returns200() throws Exception {
//            String plainBody = "ResultCode=0&ResultDesc=Success";
//            doNothing().when(paymentService).handleMpesaCallback(any());
//
//            mockMvc.perform(post(BASE_URL + "/mpesa/callback")
//                            .with(csrf())
//                            .contentType(MediaType.TEXT_PLAIN)
//                            .content(plainBody))
//                    .andExpect(status().isOk());
//
//            verify(paymentService).handleMpesaCallback(plainBody);
//        }
//
//        @Test
//        @DisplayName("endpoint is publicly accessible — no authentication required")
//        void mpesaCallback_noAuth_returns200() throws Exception {
//            // Webhook endpoints must be open to external callers (M-PESA servers)
//            doNothing().when(paymentService).handleMpesaCallback(any());
//
//            mockMvc.perform(post(BASE_URL + "/mpesa/callback")
//                            .with(csrf())
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(CALLBACK_BODY))
//                    .andExpect(status().isOk());
//        }
//
//        @Test
//        @DisplayName("propagates service exception to the caller")
//        void mpesaCallback_serviceThrows_propagatesException() throws Exception {
//            doThrow(new RuntimeException("Callback processing failed"))
//                    .when(paymentService).handleMpesaCallback(any());
//
//            mockMvc.perform(post(BASE_URL + "/mpesa/callback")
//                            .with(csrf())
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(CALLBACK_BODY))
//                    .andExpect(status().isInternalServerError());
//        }
//    }
//}