package dev.portfolio.payment.application;

import dev.portfolio.payment.domain.IdempotencyKey;
import dev.portfolio.payment.domain.Money;
import dev.portfolio.payment.domain.Payment;
import dev.portfolio.payment.infrastructure.persistence.InMemoryIdempotencyRepository;
import dev.portfolio.payment.infrastructure.persistence.InMemoryPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PaymentServiceTest {

    private static final Money TEN_DOLLARS = new Money(
            new BigDecimal("10.00"),
            Currency.getInstance("USD")
    );

    private InMemoryPaymentRepository repository;
    private PaymentService service;
    private InMemoryIdempotencyRepository idempotencyRepository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPaymentRepository();
        idempotencyRepository = new InMemoryIdempotencyRepository();
        service = new PaymentService(repository, idempotencyRepository);
    }

    @Test
    void paymentIsCreatedAndSaved(){
        Payment payment = service.createPayment(new IdempotencyKey("request-1"),TEN_DOLLARS);
        assertThat(payment.getId()).isNotNull();
        assertThat(repository.findById(payment.getId())).containsSame(payment);
    }

    @Test
    void existingPaymentCanBeRetrieved(){
        Payment createdPayment = service.createPayment(new IdempotencyKey("request-1"),TEN_DOLLARS);
        Payment foundPayment = service.getPayment(createdPayment.getId());
        assertThat(foundPayment).isSameAs(createdPayment);
    }

    @Test
    void unknownPaymentIdIsRejected(){
        UUID unknownId = UUID.randomUUID();
        assertThatThrownBy(()->
                service.getPayment(unknownId))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("Payment not found: "+unknownId);
    }

    @Test
    void sameKeyAndPaymentDetailsReturnExistingPayment(){
        IdempotencyKey key = new IdempotencyKey("duplicate-request");
        Payment firstPayment = service.createPayment(key,TEN_DOLLARS);
        Payment secondPayment = service.createPayment(key,TEN_DOLLARS);
        assertThat(secondPayment).isSameAs(firstPayment);
        assertThat(secondPayment.getId()).isEqualTo(firstPayment.getId());
    }

    @Test
    void sameKeyWithDifferentPaymentDetailsIsRejected(){
        IdempotencyKey key = new IdempotencyKey("conflicting-request");
        service.createPayment(key,TEN_DOLLARS);

        Money twentyDollars = new Money(new BigDecimal("20.00"),Currency.getInstance("USD"));

        assertThatThrownBy(()-> service.createPayment(key,twentyDollars))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessage("Idempotency key was already used with "+
                        "different payment details: conflicting-request");
    }
}
