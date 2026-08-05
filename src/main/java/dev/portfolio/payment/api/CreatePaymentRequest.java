package dev.portfolio.payment.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreatePaymentRequest (
    @NotNull(message = "amount is required")
    @Positive(message="amount must be greater than zero")
    BigDecimal amount,

    @NotBlank(message="currency is required")
    @Pattern(
            regexp="^[A-Z]{3}$",
            message="currency must be a 3-letter uppercase code"
    )
    String currency
) {
}
