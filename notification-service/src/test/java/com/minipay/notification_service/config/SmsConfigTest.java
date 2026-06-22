package com.minipay.notification_service.config;

import com.minipay.notification_service.sms.AfricasTalkingSmsProvider;
import com.minipay.notification_service.sms.ConsoleSmsProvider;
import com.minipay.notification_service.sms.SmsProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsConfigTest {

    @Mock
    private AfricasTalkingSmsProvider africasTalkingProvider;

    // -----------------------------------------------------------------------
    // Default placeholder key → ConsoleSmsProvider
    // -----------------------------------------------------------------------

    @Test
    void smsProvider_shouldReturnConsoleSmsProvider_whenApiKeyIsDefault() {
        SmsConfig config = new SmsConfig();
        ReflectionTestUtils.setField(config, "apiKey", "your-api-key");

        SmsProvider result = config.smsProvider(africasTalkingProvider);

        assertThat(result).isInstanceOf(ConsoleSmsProvider.class);
        verifyNoInteractions(africasTalkingProvider);
    }

    // -----------------------------------------------------------------------
    // Blank key → ConsoleSmsProvider
    // -----------------------------------------------------------------------

    @Test
    void smsProvider_shouldReturnConsoleSmsProvider_whenApiKeyIsBlank() {
        SmsConfig config = new SmsConfig();
        ReflectionTestUtils.setField(config, "apiKey", "   ");

        SmsProvider result = config.smsProvider(africasTalkingProvider);

        assertThat(result).isInstanceOf(ConsoleSmsProvider.class);
        verifyNoInteractions(africasTalkingProvider);
    }

    // -----------------------------------------------------------------------
    // Empty string key → ConsoleSmsProvider
    // -----------------------------------------------------------------------

    @Test
    void smsProvider_shouldReturnConsoleSmsProvider_whenApiKeyIsEmpty() {
        SmsConfig config = new SmsConfig();
        ReflectionTestUtils.setField(config, "apiKey", "");

        SmsProvider result = config.smsProvider(africasTalkingProvider);

        assertThat(result).isInstanceOf(ConsoleSmsProvider.class);
        verifyNoInteractions(africasTalkingProvider);
    }

    // -----------------------------------------------------------------------
    // Real key → AfricasTalkingSmsProvider
    // -----------------------------------------------------------------------

    @Test
    void smsProvider_shouldReturnAfricasTalkingProvider_whenApiKeyIsConfigured() {
        SmsConfig config = new SmsConfig();
        ReflectionTestUtils.setField(config, "apiKey", "real-api-key-12345");

        SmsProvider result = config.smsProvider(africasTalkingProvider);

        assertThat(result).isSameAs(africasTalkingProvider);
    }

    // -----------------------------------------------------------------------
    // Real key — ConsoleSmsProvider is never instantiated
    // -----------------------------------------------------------------------

    @Test
    void smsProvider_shouldNotReturnConsoleSmsProvider_whenApiKeyIsConfigured() {
        SmsConfig config = new SmsConfig();
        ReflectionTestUtils.setField(config, "apiKey", "real-api-key-12345");

        SmsProvider result = config.smsProvider(africasTalkingProvider);

        assertThat(result).isNotInstanceOf(ConsoleSmsProvider.class);
    }
}