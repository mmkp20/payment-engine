package dev.portfolio.payment.application;

import dev.portfolio.payment.domain.IdempotencyKey;
import dev.portfolio.payment.domain.Money;
import dev.portfolio.payment.domain.Payment;
import dev.portfolio.payment.infrastructure.persistence.InMemoryIdempotencyRepository;
import dev.portfolio.payment.infrastructure.persistence.InMemoryPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PaymentServiceTest {

    private static final Money TEN_DOLLARS = new Money(
            new BigDecimal("10.00"),
            Currency.getInstance("USD")
    );

    private InMemoryPaymentRepository repository;
    private PaymentService paymentService;
    private InMemoryIdempotencyRepository idempotencyRepository;

    private List<PaymentCreatedEvent> savedEvents;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPaymentRepository();
        idempotencyRepository =
                new InMemoryIdempotencyRepository();

        savedEvents = new ArrayList<>();

        paymentService = new PaymentService(
                repository,
                idempotencyRepository,
                savedEvents::add
        );
    }

    @Test
    void paymentIsCreatedAndSaved(){
        Payment payment = paymentService.createPayment(new IdempotencyKey("request-1"),TEN_DOLLARS);
        assertThat(payment.getId()).isNotNull();
        assertThat(repository.findById(payment.getId())).containsSame(payment);
    }

    @Test
    void existingPaymentCanBeRetrieved(){
        Payment createdPayment = paymentService.createPayment(new IdempotencyKey("request-1"),TEN_DOLLARS);
        Payment foundPayment = paymentService.getPayment(createdPayment.getId());
        assertThat(foundPayment).isSameAs(createdPayment);
    }

    @Test
    void unknownPaymentIdIsRejected(){
        UUID unknownId = UUID.randomUUID();
        assertThatThrownBy(()->
                paymentService.getPayment(unknownId))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("Payment not found: "+unknownId);
    }

    @Test
    void sameKeyAndPaymentDetailsReturnExistingPayment(){
        IdempotencyKey key = new IdempotencyKey("duplicate-request");
        Payment firstPayment = paymentService.createPayment(key,TEN_DOLLARS);
        Payment secondPayment = paymentService.createPayment(key,TEN_DOLLARS);
        assertThat(secondPayment).isSameAs(firstPayment);
        assertThat(secondPayment.getId()).isEqualTo(firstPayment.getId());
        assertThat(savedEvents).hasSize(1);
    }

    @Test
    void sameKeyWithDifferentPaymentDetailsIsRejected(){
        IdempotencyKey key = new IdempotencyKey("conflicting-request");
        paymentService.createPayment(key,TEN_DOLLARS);

        Money twentyDollars = new Money(new BigDecimal("20.00"),Currency.getInstance("USD"));

        assertThatThrownBy(()-> paymentService.createPayment(key,twentyDollars))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessage("Idempotency key was already used with "+
                        "different payment details: conflicting-request");
    }

    @Test
    void newPaymentCreatesOutboxEvent() {
        Payment payment = paymentService.createPayment(
                new IdempotencyKey("event-request"),
                TEN_DOLLARS
        );

        assertThat(savedEvents).hasSize(1);

        PaymentCreatedEvent event = savedEvents.get(0);

        assertThat(event.eventId()).isNotNull();
        assertThat(event.paymentId()).isEqualTo(payment.getId());
        assertThat(event.amount()).isEqualByComparingTo("10.00");
        assertThat(event.currency()).isEqualTo("USD");
        assertThat(event.occurredAt()).isNotNull();
    }
}
