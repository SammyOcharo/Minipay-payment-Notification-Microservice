//package com.minipay.payment_service.exception;
//
//import com.minipay.payment_service.dto.PaymentRequest;
//import com.minipay.payment_service.enums.PaymentMethod;
//import jakarta.validation.Valid;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
//import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.math.BigDecimal;
//
//import static org.hamcrest.Matchers.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
//
///**
// * Tests GlobalExceptionHandler via a minimal stub controller.
// *
// * Security autoconfiguration is excluded entirely so the JWT filter chain
// * (JwtAuthenticationFilter / JwtTokenValidator) is never instantiated.
// * No csrf() / user() post-processors are needed.
// */
//@WebMvcTest(
//        controllers = GlobalExceptionHandlerTest.StubController.class,
//        excludeAutoConfiguration = {
//                SecurityAutoConfiguration.class,
//                SecurityFilterAutoConfiguration.class
//        }
//)
//@Import(GlobalExceptionHandler.class)
//@DisplayName("GlobalExceptionHandler")
//class GlobalExceptionHandlerTest {
//
//    // ----------------------------------------------------------------
//    // Stub controller — one entry point per exception type
//    // ----------------------------------------------------------------
//
//    @RestController
//    @RequestMapping("/test")
//    static class StubController {
//
//        @PostMapping("/validate")
//        public void validate(@Valid @RequestBody PaymentRequest body) {
//            // triggers MethodArgumentNotValidException on constraint violations
//        }
//
//        @GetMapping("/response-status")
//        public void responseStatus(
//                @RequestParam(defaultValue = "400") int status,
//                @RequestParam(defaultValue = "Bad reason") String reason) {
//            throw new ResponseStatusException(HttpStatus.valueOf(status), reason);
//        }
//
//        @GetMapping("/general-error")
//        public void generalError() {
//            throw new RuntimeException("Something exploded");
//        }
//    }
//
//    // ----------------------------------------------------------------
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    // ================================================================
//    // handleValidation — MethodArgumentNotValidException
//    // ================================================================
//
//    @Nested
//    @DisplayName("handleValidation — MethodArgumentNotValidException")
//    class HandleValidation {
//
//        private static final String URL = "/test/validate";
//
//        @Test
//        @DisplayName("returns 400 BAD_REQUEST on validation failure")
//        void validation_returns400() throws Exception {
//            mockMvc.perform(post(URL)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content("{}"))
//                    .andExpect(status().isBadRequest());
//        }
//
//        @Test
//        @DisplayName("response contains 'message' = 'Validation failed'")
//        void validation_messageIsValidationFailed() throws Exception {
//            mockMvc.perform(post(URL)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content("{}"))
//                    .andExpect(jsonPath("$.message").value("Validation failed"));
//        }
//
//        @Test
//        @DisplayName("response contains a non-empty 'timestamp' string")
//        void validation_responseContainsTimestamp() throws Exception {
//            mockMvc.perform(post(URL)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content("{}"))
//                    .andExpect(jsonPath("$.timestamp").isString())
//                    .andExpect(jsonPath("$.timestamp", not(emptyString())));
//        }
//
//        @Test
//        @DisplayName("response contains 'details' map with per-field errors")
//        void validation_responseContainsDetailsMap() throws Exception {
//            mockMvc.perform(post(URL)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content("{}"))
//                    .andExpect(jsonPath("$.details").isMap())
//                    .andExpect(jsonPath("$.details").isNotEmpty());
//        }
//
//        @Test
//        @DisplayName("'amount' @NotNull → correct message")
//        void validation_amountNull_correctMessage() throws Exception {
//            mockMvc.perform(post(URL)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content("{}"))
//                    .andExpect(jsonPath("$.details.amount")
//                            .value("Amount is required"));
//        }
//
//        @Test
//        @DisplayName("'currency' @NotBlank → correct message")
//        void validation_currencyBlank_correctMessage() throws Exception {
//            mockMvc.perform(post(URL)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content("{}"))
//                    .andExpect(jsonPath("$.details.currency")
//                            .value("Currency is required"));
//        }
//
//        @Test
//        @DisplayName("'phoneNumber' @NotBlank → correct message")
//        void validation_phoneNumberBlank_correctMessage() throws Exception {
//            mockMvc.perform(post(URL)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content("{}"))
//                    .andExpect(jsonPath("$.details.phoneNumber")
//                            .value("Phone number is required"));
//        }
//
//        @Test
//        @DisplayName("'paymentMethod' @NotNull → correct message")
//        void validation_paymentMethodNull_correctMessage() throws Exception {
//            mockMvc.perform(post(URL)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content("{}"))
//                    .andExpect(jsonPath("$.details.paymentMethod")
//                            .value("Payment method is required"));
//        }
//
//        @Test
//        @DisplayName("amount below @DecimalMin → 'Minimum amount is 1.00'")
//        void validation_amountBelowMin_correctMessage() throws Exception {
//            String body = """
//                    {"amount":0.50,"currency":"KES",
//                     "phoneNumber":"+254712345678","paymentMethod":"MPESA"}""";
//
//            mockMvc.perform(post(URL)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(body))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.details.amount")
//                            .value("Minimum amount is 1.00"));
//        }
//
//        @Test
//        @DisplayName("amount above @DecimalMax → 'Maximum amount is 150,000'")
//        void validation_amountAboveMax_correctMessage() throws Exception {
//            String body = """
//                    {"amount":200000.00,"currency":"KES",
//                     "phoneNumber":"+254712345678","paymentMethod":"MPESA"}""";
//
//            mockMvc.perform(post(URL)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(body))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.details.amount")
//                            .value("Maximum amount is 150,000"));
//        }
//
//        @Test
//        @DisplayName("invalid currency pattern → correct @Pattern message")
//        void validation_invalidCurrency_correctMessage() throws Exception {
//            String body = """
//                    {"amount":500.00,"currency":"ke",
//                     "phoneNumber":"+254712345678","paymentMethod":"MPESA"}""";
//
//            mockMvc.perform(post(URL)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(body))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.details.currency")
//                            .value("Currency must be a 3-letter code e.g KES, USD"));
//        }
//
//        @Test
//        @DisplayName("invalid phone number → correct @Pattern message")
//        void validation_invalidPhoneNumber_correctMessage() throws Exception {
//            String body = """
//                    {"amount":500.00,"currency":"KES",
//                     "phoneNumber":"not-a-phone","paymentMethod":"MPESA"}""";
//
//            mockMvc.perform(post(URL)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(body))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.details.phoneNumber")
//                            .value("Phone number must be valid e.g +254712345678"));
//        }
//
//        @Test
//        @DisplayName("valid request does NOT trigger the validation handler")
//        void validation_validRequest_noError() throws Exception {
//            String body = """
//                    {"amount":500.00,"currency":"KES",
//                     "phoneNumber":"+254712345678","paymentMethod":"MPESA"}""";
//
//            mockMvc.perform(post(URL)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(body))
//                    .andExpect(status().isOk());
//        }
//
//        @Test
//        @DisplayName("optional 'description' absent does not trigger validation error")
//        void validation_noDescription_isValid() throws Exception {
//            String body = """
//                    {"amount":100.00,"currency":"USD",
//                     "phoneNumber":"+254712345678","paymentMethod":"MPESA"}""";
//
//            mockMvc.perform(post(URL)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content(body))
//                    .andExpect(status().isOk());
//        }
//    }
//
//    // ================================================================
//    // handleResponseStatus — ResponseStatusException
//    // ================================================================
//
//    @Nested
//    @DisplayName("handleResponseStatus — ResponseStatusException")
//    class HandleResponseStatus {
//
//        @Test
//        @DisplayName("mirrors the status code from the exception (404)")
//        void responseStatus_404_mirrored() throws Exception {
//            mockMvc.perform(get("/test/response-status")
//                            .param("status", "404")
//                            .param("reason", "Not found"))
//                    .andExpect(status().isNotFound());
//        }
//
//        @Test
//        @DisplayName("response 'message' equals the exception reason")
//        void responseStatus_messageEqualsReason() throws Exception {
//            mockMvc.perform(get("/test/response-status")
//                            .param("status", "409")
//                            .param("reason", "Duplicate payment"))
//                    .andExpect(status().isConflict())
//                    .andExpect(jsonPath("$.message").value("Duplicate payment"));
//        }
//
//        @Test
//        @DisplayName("response contains a non-empty 'timestamp' string")
//        void responseStatus_responseContainsTimestamp() throws Exception {
//            mockMvc.perform(get("/test/response-status"))
//                    .andExpect(jsonPath("$.timestamp").isString())
//                    .andExpect(jsonPath("$.timestamp", not(emptyString())));
//        }
//
//        @Test
//        @DisplayName("response does NOT contain 'details' (null details param)")
//        void responseStatus_noDetailsField() throws Exception {
//            mockMvc.perform(get("/test/response-status"))
//                    .andExpect(jsonPath("$.details").doesNotExist());
//        }
//
//        @Test
//        @DisplayName("handles 400 ResponseStatusException correctly")
//        void responseStatus_400_handled() throws Exception {
//            mockMvc.perform(get("/test/response-status")
//                            .param("status", "400")
//                            .param("reason", "Bad input"))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.message").value("Bad input"));
//        }
//
//        @Test
//        @DisplayName("handles 403 ResponseStatusException correctly")
//        void responseStatus_403_handled() throws Exception {
//            mockMvc.perform(get("/test/response-status")
//                            .param("status", "403")
//                            .param("reason", "Forbidden resource"))
//                    .andExpect(status().isForbidden())
//                    .andExpect(jsonPath("$.message").value("Forbidden resource"));
//        }
//
//        @Test
//        @DisplayName("handles 500 ResponseStatusException correctly")
//        void responseStatus_500_handled() throws Exception {
//            mockMvc.perform(get("/test/response-status")
//                            .param("status", "500")
//                            .param("reason", "Internal failure"))
//                    .andExpect(status().isInternalServerError())
//                    .andExpect(jsonPath("$.message").value("Internal failure"));
//        }
//    }
//
//    // ================================================================
//    // handleGeneral — Exception
//    // ================================================================
//
//    @Nested
//    @DisplayName("handleGeneral — Exception")
//    class HandleGeneral {
//
//        @Test
//        @DisplayName("returns 500 INTERNAL_SERVER_ERROR for unhandled exceptions")
//        void general_returns500() throws Exception {
//            mockMvc.perform(get("/test/general-error"))
//                    .andExpect(status().isInternalServerError());
//        }
//
//        @Test
//        @DisplayName("response 'message' is the generic safe message")
//        void general_messageIsGenericSafeMessage() throws Exception {
//            mockMvc.perform(get("/test/general-error"))
//                    .andExpect(jsonPath("$.message")
//                            .value("An unexpected error occurred"));
//        }
//
//        @Test
//        @DisplayName("response contains a non-empty 'timestamp' string")
//        void general_responseContainsTimestamp() throws Exception {
//            mockMvc.perform(get("/test/general-error"))
//                    .andExpect(jsonPath("$.timestamp").isString())
//                    .andExpect(jsonPath("$.timestamp", not(emptyString())));
//        }
//
//        @Test
//        @DisplayName("raw exception message is NOT exposed in the response")
//        void general_doesNotExposeExceptionMessage() throws Exception {
//            mockMvc.perform(get("/test/general-error"))
//                    .andExpect(jsonPath("$.details").doesNotExist())
//                    .andExpect(jsonPath("$.message",
//                            not(containsString("Something exploded"))));
//        }
//    }
//
//    // ================================================================
//    // buildError — shared response structure
//    // ================================================================
//
//    @Nested
//    @DisplayName("buildError — shared response structure")
//    class BuildError {
//
//        @Test
//        @DisplayName("'details' absent when details param is null (ResponseStatusException path)")
//        void buildError_detailsAbsentWhenNull() throws Exception {
//            mockMvc.perform(get("/test/response-status")
//                            .param("status", "404")
//                            .param("reason", "Missing"))
//                    .andExpect(jsonPath("$.details").doesNotExist());
//        }
//
//        @Test
//        @DisplayName("'details' present when details param is non-null (validation path)")
//        void buildError_detailsPresentWhenNonNull() throws Exception {
//            mockMvc.perform(post("/test/validate")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content("{}"))
//                    .andExpect(jsonPath("$.details").exists());
//        }
//
//        @Test
//        @DisplayName("timestamp is present on all three handler paths")
//        void buildError_timestampPresentOnAllPaths() throws Exception {
//            // validation path
//            mockMvc.perform(post("/test/validate")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .content("{}"))
//                    .andExpect(jsonPath("$.timestamp").isString());
//
//            // ResponseStatusException path
//            mockMvc.perform(get("/test/response-status"))
//                    .andExpect(jsonPath("$.timestamp").isString());
//
//            // general Exception path
//            mockMvc.perform(get("/test/general-error"))
//                    .andExpect(jsonPath("$.timestamp").isString());
//        }
//    }
//}