package dev.portfolio.payment.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PaymentTest {

    private static final Money TEN_DOLLARS = new Money(
            new BigDecimal("10.00"),
            Currency.getInstance("USD")
    );

    @Test
    public void newPaymentStartsInCreatedStatus(){
        Payment payment = Payment.create(UUID.randomUUID(),TEN_DOLLARS);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CREATED);
    }

    @Test
    public void createdPaymentCanStartProcessing(){
        Payment payment = Payment.create(UUID.randomUUID(),TEN_DOLLARS);
        payment.startProcessing();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    }

    @Test
    public void processingPaymentCanSucceed(){
        Payment payment = Payment.create(UUID.randomUUID(),TEN_DOLLARS);
        payment.startProcessing();
        payment.markSucceeded();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    public void processingPaymentCanFail(){
        Payment payment = Payment.create(UUID.randomUUID(),TEN_DOLLARS);
        payment.startProcessing();
        payment.markFailed();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    public void createdPaymentCannotSucceedDirectly(){
        Payment payment = Payment.create(UUID.randomUUID(),TEN_DOLLARS);
        assertThatThrownBy(payment::markSucceeded)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payment must be PROCESSING but was CREATED");
    }

    @Test
    public void succeededPaymentCannotBeProcessedAgain(){
        Payment payment = Payment.create(UUID.randomUUID(),TEN_DOLLARS);
        payment.startProcessing();
        payment.markSucceeded();
        assertThatThrownBy(payment::startProcessing)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payment must be CREATED but was SUCCEEDED");
    }
    @Test
    public void newPaymentKeepsItsMoney(){
        Payment payment = Payment.create(UUID.randomUUID(),TEN_DOLLARS);
        assertThat(payment.getMoney()).isEqualTo(TEN_DOLLARS);
    }
}
