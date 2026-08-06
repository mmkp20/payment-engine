package dev.portfolio.payment.infrastructure.persistence;

import dev.portfolio.payment.domain.Money;
import dev.portfolio.payment.domain.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class InMemoryPaymentRepositoryTest {

    private final InMemoryPaymentRepository repository =
            new InMemoryPaymentRepository();

    @Test
    public void savedPaymentCanBeFoundById(){
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.create(
                paymentId,
                new Money(new BigDecimal("10.99"),
                        Currency.getInstance("USD"))
        );
        repository.save(payment);
        Optional<Payment> foundPayment = repository.findById(paymentId);
        assertThat(foundPayment).containsSame(payment);
    }

    @Test
    public void unknownPaymentIdReturnsEmpty(){
        Optional<Payment> foundPayment =
                repository.findById(UUID.randomUUID());
        assertThat(foundPayment).isEmpty();
    }
}
