package com.minipay.notification_service.sms;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SmsResponse {
    private boolean success;
    private String messageId;
    private String status;
    private String failureReason;
}
