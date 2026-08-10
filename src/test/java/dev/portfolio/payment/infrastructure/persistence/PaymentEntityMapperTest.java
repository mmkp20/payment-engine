package dev.portfolio.payment.infrastructure.persistence;

import dev.portfolio.payment.domain.Money;
import dev.portfolio.payment.domain.Payment;
import dev.portfolio.payment.domain.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEntityMapperTest {

    @Test
    void domainPaymentIsConvertedToEntity() {
        UUID paymentId = UUID.randomUUID();

        Payment payment = Payment.create(
                paymentId,
                new Money(
                        new BigDecimal("10.99"),
                        Currency.getInstance("USD")
                )
        );

        PaymentEntity entity =
                PaymentEntityMapper.fromDomain(payment);

        assertThat(entity.getId()).isEqualTo(paymentId);
        assertThat(entity.getAmount())
                .isEqualByComparingTo("10.99");
        assertThat(entity.getCurrency()).isEqualTo("USD");
        assertThat(entity.getStatus())
                .isEqualTo(PaymentStatus.CREATED);
    }

    @Test
    void entityIsRestoredToDomainPayment() {
        UUID paymentId = UUID.randomUUID();

        PaymentEntity entity = new PaymentEntity(
                paymentId,
                new BigDecimal("25.00"),
                "USD",
                PaymentStatus.PROCESSING
        );

        Payment payment =
                PaymentEntityMapper.toDomain(entity);

        assertThat(payment.getId()).isEqualTo(paymentId);
        assertThat(payment.getMoney().amount())
                .isEqualByComparingTo("25.00");
        assertThat(payment.getMoney().currency())
                .isEqualTo(Currency.getInstance("USD"));
        assertThat(payment.getStatus())
                .isEqualTo(PaymentStatus.PROCESSING);
    }
}