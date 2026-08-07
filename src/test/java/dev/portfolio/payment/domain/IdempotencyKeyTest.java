package dev.portfolio.payment.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IdempotencyKeyTest {

    @Test
    void validKeyIsCreated(){
        IdempotencyKey key = new IdempotencyKey("checkout-request-123");
        assertThat(key.value()).isEqualTo("checkout-request-123");
    }

    @Test
    void nullKeyIsRejected(){
        assertThatThrownBy(()->
                new IdempotencyKey(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("idempotencyKey must not be null");
    }

    @Test
    void blankKeyIsRejected(){
        assertThatThrownBy(() ->
                new IdempotencyKey("    "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("idempotencyKey must not be blank");
    }
}
