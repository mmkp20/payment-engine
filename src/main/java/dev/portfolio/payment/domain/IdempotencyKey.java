package dev.portfolio.payment.domain;

import java.util.Objects;

public record IdempotencyKey(String value) {

    public IdempotencyKey {
        Objects.requireNonNull(
                value,
                "idempotencyKey must not be null"
        );

        if(value.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }
}
