package dev.portfolio.payment.infrastructure.persistence;

import dev.portfolio.payment.domain.IdempotencyKey;
import dev.portfolio.payment.domain.Money;
import dev.portfolio.payment.domain.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryIdempotencyRepositoryTest {

    private final InMemoryIdempotencyRepository repository = new InMemoryIdempotencyRepository();

    @Test
    void savedPaymentCanBeFoundByKey(){
        IdempotencyKey key = new IdempotencyKey("checkout-request-123");
        Payment payment = Payment.create(UUID.randomUUID(),
                                        new Money(new BigDecimal("10.00"),
                                        Currency.getInstance("USD")));

        repository.save(key, payment);
        assertThat(repository.findByKey(key)).containsSame(payment);
    }

    @Test
    void unknownKeyReturnsEmpty(){
        IdempotencyKey unknownKey = new IdempotencyKey("unknown-request");
        assertThat(repository.findByKey(unknownKey)).isEmpty();
    }
}
