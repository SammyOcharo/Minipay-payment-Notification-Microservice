package com.minipay.payment_service.dto;

import com.minipay.payment_service.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum amount is 1.00")
    @DecimalMax(value = "150000.00", message = "Maximum amount is 150,000")
    @Digits(integer = 15, fraction = 4,
            message = "Invalid amount format")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$",
            message = "Currency must be a 3-letter code e.g KES, USD")
    private String currency;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^\\+?[1-9]\\d{1,14}$",
            message = "Phone number must be valid e.g +254712345678"
    )
    private String phoneNumber;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String description;
}
