package dev.portfolio.payment.application;

import dev.portfolio.payment.domain.IdempotencyKey;

public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(IdempotencyKey key) {
        super("Idempotency key was already used with " +
                "different payment details: " + key.value());
    }
}
