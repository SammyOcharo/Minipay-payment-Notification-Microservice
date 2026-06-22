package com.minipay.notification_service.sms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleSmsProviderTest {

    private ConsoleSmsProvider provider;

    @BeforeEach
    void setUp() {
        provider = new ConsoleSmsProvider();
    }

    @Test
    void sendSms_shouldReturnSuccess() {
        SmsResponse response = provider.sendSms("+254712345678", "Test message");

        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void sendSms_shouldReturnConsoleSentStatus() {
        SmsResponse response = provider.sendSms("+254712345678", "Test message");

        assertThat(response.getStatus()).isEqualTo("CONSOLE_SENT");
    }

    @Test
    void sendSms_shouldReturnMessageIdWithConsolePrefix() {
        SmsResponse response = provider.sendSms("+254712345678", "Test message");

        assertThat(response.getMessageId()).startsWith("CONSOLE-");
    }

    @Test
    void sendSms_shouldReturnUniqueMessageIds_onSubsequentCalls() throws InterruptedException {
        SmsResponse first = provider.sendSms("+254712345678", "First");
        Thread.sleep(2); // ensure System.currentTimeMillis() differs
        SmsResponse second = provider.sendSms("+254712345678", "Second");

        assertThat(first.getMessageId()).isNotEqualTo(second.getMessageId());
    }

    @Test
    void sendSms_shouldReturnNullFailureReason() {
        SmsResponse response = provider.sendSms("+254712345678", "Test message");

        assertThat(response.getFailureReason()).isNull();
    }

    @Test
    void sendSms_shouldHandleNullPhoneNumber() {
        SmsResponse response = provider.sendSms(null, "Test message");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getStatus()).isEqualTo("CONSOLE_SENT");
    }

    @Test
    void sendSms_shouldHandleNullMessage() {
        SmsResponse response = provider.sendSms("+254712345678", null);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getStatus()).isEqualTo("CONSOLE_SENT");
    }

    @Test
    void sendSms_shouldHandleEmptyStrings() {
        SmsResponse response = provider.sendSms("", "");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getStatus()).isEqualTo("CONSOLE_SENT");
    }
}