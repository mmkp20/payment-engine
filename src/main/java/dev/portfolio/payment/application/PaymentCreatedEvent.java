package dev.portfolio.payment.application;

import dev.portfolio.payment.domain.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCreatedEvent(
        UUID eventId,
        UUID paymentId,
        BigDecimal amount,
        String currency,
        Instant occurredAt
) {
    public static PaymentCreatedEvent from(Payment payment) {
        return new PaymentCreatedEvent(
                UUID.randomUUID(),
                payment.getId(),
                payment.getMoney().amount(),
                payment.getMoney().currency().getCurrencyCode(),
                Instant.now()
        );
    }
}