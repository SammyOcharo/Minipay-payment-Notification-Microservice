package com.minipay.notification_service.sms;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class AfricasTalkingSmsProvider implements SmsProvider {

    @Value("${africastalking.username}")
    private String username;

    @Value("${africastalking.api-key}")
    private String apiKey;

    @Value("${africastalking.sms-url}")
    private String smsUrl;

    @Value("${africastalking.sender-id:}")
    private String senderId;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public SmsResponse sendSms(String phoneNumber, String message) {
        log.info("Sending SMS via Africa's Talking to: {}", phoneNumber);

        try {
            RequestBody body = new FormBody.Builder()
                    .add("username", username)
                    .add("to", phoneNumber)
                    .add("message", message)
                    .build();

            Request request = new Request.Builder()
                    .url(smsUrl)
                    .post(body)
                    .addHeader("apiKey", apiKey)
                    .addHeader("Accept", "application/json")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null
                        ? response.body().string() : "";

                if (response.isSuccessful()) {
                    log.info("SMS sent successfully to: {} response: {}",
                            phoneNumber, responseBody);
                    return SmsResponse.builder()
                            .success(true)
                            .status("SUCCESS")
                            .messageId(extractMessageId(responseBody))
                            .build();
                } else {
                    log.error("SMS failed. Status: {} Body: {}",
                            response.code(), responseBody);
                    return SmsResponse.builder()
                            .success(false)
                            .status("FAILED")
                            .failureReason("HTTP " + response.code()
                                    + ": " + responseBody)
                            .build();
                }
            }
        } catch (IOException e) {
            log.error("SMS send error for {}: {}", phoneNumber,
                    e.getMessage());
            return SmsResponse.builder()
                    .success(false)
                    .status("ERROR")
                    .failureReason(e.getMessage())
                    .build();
        }
    }

    private String extractMessageId(String responseBody) {
        // Basic extraction — upgrade to proper JSON parsing if needed
        if (responseBody.contains("messageId")) {
            try {
                int start = responseBody.indexOf("messageId") + 12;
                int end = responseBody.indexOf("\"", start);
                return responseBody.substring(start, end);
            } catch (Exception e) {
                return "UNKNOWN";
            }
        }
        return "UNKNOWN";
    }
}