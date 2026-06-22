package com.minipay.notification_service.config;

import com.minipay.notification_service.sms.AfricasTalkingSmsProvider;
import com.minipay.notification_service.sms.ConsoleSmsProvider;
import com.minipay.notification_service.sms.SmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SmsConfig {

    @Value("${africastalking.api-key:your-api-key}")
    private String apiKey;

    @Bean
    public SmsProvider smsProvider(AfricasTalkingSmsProvider africasTalkingProvider) {
        if (apiKey.equals("your-api-key") || apiKey.isBlank()) {
            log.warn("Africa's Talking API key not configured. " +
                    "Using console SMS provider.");
            return new ConsoleSmsProvider();
        }
        log.info("Using Africa's Talking SMS provider.");
        return africasTalkingProvider;
    }
}