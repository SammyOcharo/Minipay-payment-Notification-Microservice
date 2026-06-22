package com.minipay.notification_service.sms;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class AfricasTalkingSmsProviderTest {

    private MockWebServer mockWebServer;
    private AfricasTalkingSmsProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        provider = new AfricasTalkingSmsProvider();
        ReflectionTestUtils.setField(provider, "username", "test-user");
        ReflectionTestUtils.setField(provider, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(provider, "smsUrl",
                mockWebServer.url("/version1/messaging").toString());
        ReflectionTestUtils.setField(provider, "senderId", "MiniPay");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // -----------------------------------------------------------------------
    // Happy path — 200 response with messageId in body
    // -----------------------------------------------------------------------

    @Test
    void sendSms_shouldReturnSuccess_whenApiReturns200WithMessageId()
            throws InterruptedException {
        String responseBody = """
                {"SMSMessageData":{"Recipients":[{"messageId":"ATXid_123","status":"Success"}]}}
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        SmsResponse response = provider.sendSms("+254712345678", "Test message");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessageId()).isEqualTo("ATXid_123");
        assertThat(response.getFailureReason()).isNull();
    }

    // -----------------------------------------------------------------------
    // Happy path — request fields sent correctly
    // -----------------------------------------------------------------------

    @Test
    void sendSms_shouldSendCorrectRequestFields() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{}")
                .addHeader("Content-Type", "application/json"));

        provider.sendSms("+254712345678", "Hello MiniPay");

        RecordedRequest recorded = mockWebServer.takeRequest();
        String requestBody = recorded.getBody().readUtf8();

        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getHeader("apiKey")).isEqualTo("test-api-key");
        assertThat(recorded.getHeader("Accept")).isEqualTo("application/json");
        assertThat(requestBody).contains("username=test-user");
        assertThat(requestBody).contains("to=%2B254712345678");
        assertThat(requestBody).contains("message=Hello%20MiniPay"); // OkHttp uses %20 not +
    }

    // -----------------------------------------------------------------------
    // 200 response but no messageId in body — falls back to UNKNOWN
    // -----------------------------------------------------------------------

    @Test
    void sendSms_shouldReturnUnknownMessageId_whenBodyHasNoMessageId() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"SMSMessageData\":{}}")
                .addHeader("Content-Type", "application/json"));

        SmsResponse response = provider.sendSms("+254712345678", "Test message");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessageId()).isEqualTo("UNKNOWN");
    }

    // -----------------------------------------------------------------------
    // extractMessageId — messageId present but malformed → UNKNOWN
    // -----------------------------------------------------------------------

    @Test
    void sendSms_shouldReturnUnknownMessageId_whenMessageIdIsMalformed() {
        // "messageId" is present but the substring extraction will throw
        // because there is no closing quote after the expected offset
        String malformedBody = "{\"messageId\":}";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(malformedBody)
                .addHeader("Content-Type", "application/json"));

        SmsResponse response = provider.sendSms("+254712345678", "Test message");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessageId()).isEqualTo("UNKNOWN");
    }

    // -----------------------------------------------------------------------
    // Non-2xx response — FAILED with HTTP status code in reason
    // -----------------------------------------------------------------------

    @Test
    void sendSms_shouldReturnFailure_whenApiReturns400() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("Bad Request")
                .addHeader("Content-Type", "application/json"));

        SmsResponse response = provider.sendSms("+254712345678", "Test message");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getFailureReason()).contains("400");
        assertThat(response.getFailureReason()).contains("Bad Request");
    }

    @Test
    void sendSms_shouldReturnFailure_whenApiReturns500() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
                .addHeader("Content-Type", "application/json"));

        SmsResponse response = provider.sendSms("+254712345678", "Test message");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getFailureReason()).contains("500");
    }

    // -----------------------------------------------------------------------
    // Non-2xx response — null body handled safely
    // -----------------------------------------------------------------------

    @Test
    void sendSms_shouldHandleEmptyBodyOnFailureResponse() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("")
                .addHeader("Content-Type", "application/json"));

        SmsResponse response = provider.sendSms("+254712345678", "Test message");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getFailureReason()).contains("401");
    }

    // -----------------------------------------------------------------------
    // IOException — network failure path
    // -----------------------------------------------------------------------

    @Test
    void sendSms_shouldReturnError_whenNetworkFails() throws IOException {
        // Shut down the server before the call to force an IOException
        mockWebServer.shutdown();

        SmsResponse response = provider.sendSms("+254712345678", "Test message");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getStatus()).isEqualTo("ERROR");
        assertThat(response.getFailureReason()).isNotBlank();
    }

    // -----------------------------------------------------------------------
    // extractMessageId — messageId present and correctly extracted
    // -----------------------------------------------------------------------

    @Test
    void sendSms_shouldExtractMessageId_whenPresentAtDifferentPosition() {
        // Verifies extraction works regardless of surrounding JSON fields
        String body = """
                {"count":1,"messageId":"MSG-XYZ-999","cost":"KES 1.00"}
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(body)
                .addHeader("Content-Type", "application/json"));

        SmsResponse response = provider.sendSms("+254712345678", "Test");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessageId()).isEqualTo("MSG-XYZ-999");
    }
}