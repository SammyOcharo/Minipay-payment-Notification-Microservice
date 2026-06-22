package com.minipay.payment_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GatewayResponse {

    private boolean success;
    private String gatewayReference;
    private String gatewayStatus;
    private String failureReason;
    private String mpesaReceiptNumber;
    private String merchantRequestId;
}