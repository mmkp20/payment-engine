package dev.portfolio.payment.api;

import dev.portfolio.payment.domain.Payment;
import dev.portfolio.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse (UUID id, BigDecimal amount, String currency, PaymentStatus status)
{
    public  static PaymentResponse from(Payment payment){
        return new PaymentResponse(
                payment.getId(),
                payment.getMoney().amount(),
                payment.getMoney().currency().getCurrencyCode(),
                payment.getStatus()
        );
    }
}