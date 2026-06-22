package com.minipay.payment_service.gateway;

import com.minipay.payment_service.dto.GatewayResponse;
import com.minipay.payment_service.enums.PaymentMethod;
import com.minipay.payment_service.model.Payment;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MpesaPaymentGateway implements PaymentGateway {

    @Value("${mpesa.base-url}")
    private String baseUrl;

    @Value("${mpesa.consumer-key}")
    private String consumerKey;

    @Value("${mpesa.consumer-secret}")
    private String consumerSecret;

    @Value("${mpesa.shortcode}")
    private String shortcode;

    @Value("${mpesa.passkey}")
    private String passkey;

    @Value("${mpesa.callback-url}")
    private String callbackUrl;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @CircuitBreaker(name = "mpesa-gateway", fallbackMethod = "fallback")
    @Retry(name = "mpesa-gateway")
    public GatewayResponse processPayment(Payment payment) {
        log.info("Processing real M-PESA STK Push for phone: {} amount: {}",
                payment.getPhoneNumber(), payment.getAmount());

        try {
            // Step 1 — Get access token
            String accessToken = getAccessToken();
            log.info("M-PESA access token obtained successfully");

            // Step 2 — Generate password and timestamp
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String password = generatePassword(timestamp);

            // Step 3 — Initiate STK Push
            return initiateStkPush(
                    payment, accessToken, timestamp, password);

        } catch (Exception e) {
            log.error("M-PESA STK Push failed: {}", e.getMessage(), e);
            return GatewayResponse.builder()
                    .success(false)
                    .gatewayStatus("FAILED")
                    .failureReason("M-PESA error: " + e.getMessage())
                    .build();
        }
    }

    private String getAccessToken() throws IOException {
        String credentials = Base64.getEncoder().encodeToString(
                (consumerKey + ":" + consumerSecret)
                        .getBytes(StandardCharsets.UTF_8));

        Request request = new Request.Builder()
                .url(baseUrl + "/oauth/v1/generate?grant_type=client_credentials")
                .get()
                .addHeader("Authorization", "Basic " + credentials)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null
                    ? response.body().string() : "";

            if (!response.isSuccessful()) {
                throw new IOException("Failed to get access token: "
                        + responseBody);
            }

            Map<?, ?> result = objectMapper.readValue(
                    responseBody, Map.class);
            return (String) result.get("access_token");
        }
    }

    private String generatePassword(String timestamp) {
        String rawPassword = shortcode + passkey + timestamp;
        return Base64.getEncoder().encodeToString(
                rawPassword.getBytes(StandardCharsets.UTF_8));
    }

    private GatewayResponse initiateStkPush(
            Payment payment,
            String accessToken,
            String timestamp,
            String password) throws IOException {

        // Format phone number — remove + prefix
        String phone = payment.getPhoneNumber()
                .replace("+", "")
                .trim();

        // Amount must be whole number for M-PESA
        int amount = payment.getAmount().intValue();

        Map<String, Object> stkPushRequest = new HashMap<>();
        stkPushRequest.put("BusinessShortCode", shortcode);
        stkPushRequest.put("Password", password);
        stkPushRequest.put("Timestamp", timestamp);
        stkPushRequest.put("TransactionType", "CustomerPayBillOnline");
        stkPushRequest.put("Amount", amount);
        stkPushRequest.put("PartyA", phone);
        stkPushRequest.put("PartyB", shortcode);
        stkPushRequest.put("PhoneNumber", phone);
        stkPushRequest.put("CallBackURL", callbackUrl);
        stkPushRequest.put("AccountReference", "MiniPay-" + payment.getId());
        stkPushRequest.put("TransactionDesc",
                payment.getDescription() != null
                        ? payment.getDescription() : "MiniPay Payment");

        String requestBody = objectMapper.writeValueAsString(stkPushRequest);

        Request request = new Request.Builder()
                .url(baseUrl + "/mpesa/stkpush/v1/processrequest")
                .post(RequestBody.create(
                        requestBody,
                        MediaType.parse("application/json")))
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null
                    ? response.body().string() : "";

            log.info("M-PESA STK Push response: {}", responseBody);

            Map<String, Object> result = objectMapper.readValue(
                    responseBody, Map.class);

            String responseCode = (String) result.get("ResponseCode");
            String checkoutRequestId = (String) result
                    .get("CheckoutRequestID");
            String merchantRequestId = (String) result
                    .get("MerchantRequestID");

            if ("0".equals(responseCode)) {
                log.info("STK Push initiated successfully. " +
                        "CheckoutRequestID: {}", checkoutRequestId);
                return GatewayResponse.builder()
                        .success(true)
                        .gatewayReference(checkoutRequestId)
                        .gatewayStatus("STK_PUSH_SENT")
                        .merchantRequestId(merchantRequestId)
                        .build();
            } else {
                String errorMessage = String.valueOf(result
                        .getOrDefault("errorMessage", "STK Push failed"));
                log.error("STK Push failed: {}", errorMessage);
                return GatewayResponse.builder()
                        .success(false)
                        .gatewayStatus("FAILED")
                        .failureReason(errorMessage)
                        .build();
            }
        }
    }

    public GatewayResponse fallback(Payment payment, Exception ex) {
        log.error("M-PESA circuit breaker open. Error: {}",
                ex.getMessage());
        return GatewayResponse.builder()
                .success(false)
                .gatewayStatus("CIRCUIT_OPEN")
                .failureReason("M-PESA service temporarily unavailable.")
                .build();
    }

    @Override
    public boolean supports(PaymentMethod method) {
        return method == PaymentMethod.MPESA;
    }
}