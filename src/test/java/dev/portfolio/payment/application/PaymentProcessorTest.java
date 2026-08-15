package dev.portfolio.payment.application;

import dev.portfolio.payment.domain.Money;
import dev.portfolio.payment.domain.Payment;
import dev.portfolio.payment.domain.PaymentStatus;
import dev.portfolio.payment.infrastructure.persistence.InMemoryPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentProcessorTest {

    private static final Money TEN_DOLLARS = new Money(
            new BigDecimal("10.00"),
            Currency.getInstance("USD")
    );

    private InMemoryPaymentRepository repository;
    private PaymentProcessor paymentProcessor;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPaymentRepository();
        paymentProcessor = new PaymentProcessor(repository);
    }

    @Test
    void createdPaymentIsProcessedSuccessfully() {
        Payment payment = Payment.create(
                UUID.randomUUID(),
                TEN_DOLLARS
        );

        repository.save(payment);

        Payment processedPayment =
                paymentProcessor.processPayment(payment.getId());

        assertThat(processedPayment.getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void processingPaymentCanResumeAndSucceed() {
        Payment payment = Payment.create(
                UUID.randomUUID(),
                TEN_DOLLARS
        );

        payment.startProcessing();
        repository.save(payment);

        Payment processedPayment =
                paymentProcessor.processPayment(payment.getId());

        assertThat(processedPayment.getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void succeededPaymentIsNotProcessedAgain() {
        Payment payment = Payment.create(
                UUID.randomUUID(),
                TEN_DOLLARS
        );

        payment.startProcessing();
        payment.markSucceeded();
        repository.save(payment);

        Payment processedPayment =
                paymentProcessor.processPayment(payment.getId());

        assertThat(processedPayment)
                .isSameAs(payment);
        assertThat(processedPayment.getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void failedPaymentIsRejected() {
        Payment payment = Payment.create(
                UUID.randomUUID(),
                TEN_DOLLARS
        );

        payment.startProcessing();
        payment.markFailed();
        repository.save(payment);

        assertThatThrownBy(() ->
                paymentProcessor.processPayment(payment.getId())
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(
                        "Failed payment cannot be processed again"
                );
    }

    @Test
    void unknownPaymentIsRejected() {
        UUID unknownId = UUID.randomUUID();

        assertThatThrownBy(() ->
                paymentProcessor.processPayment(unknownId)
        )
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessage("Payment not found: " + unknownId);
    }
}