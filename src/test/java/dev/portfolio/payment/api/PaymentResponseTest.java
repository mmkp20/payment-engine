package dev.portfolio.payment.api;

import dev.portfolio.payment.domain.Money;
import dev.portfolio.payment.domain.Payment;
import dev.portfolio.payment.domain.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class PaymentResponseTest {

    @Test
    void paymentIsConvertedToResponse() {
        UUID paymentId = UUID.randomUUID();

        Money money = new Money(
                new BigDecimal("10.99"),
                Currency.getInstance("USD"));

        Payment payment = Payment.create(paymentId, money);
        PaymentResponse response = PaymentResponse.from(payment);

        assertThat(response.id()).isEqualTo(paymentId);
        assertThat(response.amount()).isEqualByComparingTo("10.99");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.status()).isEqualTo(PaymentStatus.CREATED);
    }
}
