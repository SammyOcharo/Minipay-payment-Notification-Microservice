package com.minipay.notification_service.sms;

public interface SmsProvider {
    SmsResponse sendSms(String phoneNumber, String message);
}