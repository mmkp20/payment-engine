package dev.portfolio.payment.application;

import dev.portfolio.payment.domain.Money;
import dev.portfolio.payment.domain.Payment;
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

    @BeforeEach
    void setUp() {
        repository = new InMemoryPaymentRepository();
        service = new PaymentService(repository);
    }

    @Test
    void paymentIsCreatedAndSaved(){
        Payment payment = service.createPayment(TEN_DOLLARS);
        assertThat(payment.getId()).isNotNull();
        assertThat(repository.findById(payment.getId())).containsSame(payment);
    }

    @Test
    void existingPaymentCanBeRetrieved(){
        Payment createdPayment = service.createPayment(TEN_DOLLARS);
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
}
