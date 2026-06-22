package com.minipay.payment_service.gateway;

import com.minipay.payment_service.dto.GatewayResponse;
import com.minipay.payment_service.model.Payment;

public interface PaymentGateway {
    GatewayResponse processPayment(Payment payment);
    boolean supports(com.minipay.payment_service.enums.PaymentMethod method);
}
