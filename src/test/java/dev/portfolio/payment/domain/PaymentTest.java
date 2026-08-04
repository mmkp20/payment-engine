package dev.portfolio.payment.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PaymentTest {

    @Test
    public void newPaymentStartsInCreatedStatus(){
        Payment payment = Payment.create(UUID.randomUUID());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CREATED);
    }

    @Test
    public void createdPaymentCanStartProcessing(){
        Payment payment = Payment.create(UUID.randomUUID());
        payment.startProcessing();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PROCESSING);
    }

    @Test
    public void processingPaymentCanSucceed(){
        Payment payment = Payment.create(UUID.randomUUID());
        payment.startProcessing();
        payment.markSucceeded();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    public void processingPaymentCanFail(){
        Payment payment = Payment.create(UUID.randomUUID());
        payment.startProcessing();
        payment.markFailed();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    public void createdPaymentCannotSucceedDirectly(){
        Payment payment = Payment.create(UUID.randomUUID());
        assertThatThrownBy(payment::markSucceeded)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payment must be PROCESSING but was CREATED");
    }

    @Test
    public void succeededPaymentCannotBeProcessedAgain(){
        Payment payment = Payment.create(UUID.randomUUID());
        payment.startProcessing();
        payment.markSucceeded();
        assertThatThrownBy(payment::startProcessing)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payment must be CREATED but was SUCCEEDED");
    }
}
