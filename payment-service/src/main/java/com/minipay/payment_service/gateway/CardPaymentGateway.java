package com.minipay.payment_service.gateway;


import com.minipay.payment_service.dto.GatewayResponse;
import com.minipay.payment_service.enums.PaymentMethod;
import com.minipay.payment_service.model.Payment;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class CardPaymentGateway implements PaymentGateway {

    @Override
    @CircuitBreaker(name = "card-gateway", fallbackMethod = "fallback")
    @Retry(name = "card-gateway")
    public GatewayResponse processPayment(Payment payment) {
        log.info("Processing Card payment for amount: {}",
                payment.getAmount());

        return simulateCardPayment(payment);
    }

    private GatewayResponse simulateCardPayment(Payment payment) {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate: amounts over 100,000 are declined
        boolean success = payment.getAmount()
                .compareTo(new BigDecimal("100000")) <= 0;

        if (success) {
            String paymentIntentId = "pi_" + UUID.randomUUID()
                    .toString().replace("-", "").substring(0, 24);

            log.info("Card payment SUCCESS. Reference: {}", paymentIntentId);

            return GatewayResponse.builder()
                    .success(true)
                    .gatewayReference(paymentIntentId)
                    .gatewayStatus("succeeded")
                    .build();
        } else {
            log.warn("Card payment DECLINED for amount: {}",
                    payment.getAmount());

            return GatewayResponse.builder()
                    .success(false)
                    .gatewayStatus("declined")
                    .failureReason("Card declined: amount exceeds limit")
                    .build();
        }
    }

    public GatewayResponse fallback(Payment payment, Exception ex) {
        log.error("Card circuit breaker open. Payment: {} Error: {}",
                payment.getId(), ex.getMessage());

        return GatewayResponse.builder()
                .success(false)
                .gatewayStatus("CIRCUIT_OPEN")
                .failureReason("Card service temporarily unavailable. " +
                        "Please try again later.")
                .build();
    }

    @Override
    public boolean supports(PaymentMethod method) {
        return method == PaymentMethod.CARD;
    }
}