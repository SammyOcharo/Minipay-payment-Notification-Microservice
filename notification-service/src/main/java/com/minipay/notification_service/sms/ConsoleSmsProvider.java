package com.minipay.notification_service.sms;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleSmsProvider implements SmsProvider {

    @Override
    public SmsResponse sendSms(String phoneNumber, String message) {
        log.info("         SMS NOTIFICATION              ");
        log.info("To:      {}", phoneNumber);
        log.info("Message: {}", message);

        return SmsResponse.builder()
                .success(true)
                .status("CONSOLE_SENT")
                .messageId("CONSOLE-" + System.currentTimeMillis())
                .build();
    }
}