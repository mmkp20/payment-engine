package dev.portfolio.payment.infrastructure.persistence;

import dev.portfolio.payment.domain.Money;
import dev.portfolio.payment.domain.Payment;

import java.util.Currency;

final class PaymentEntityMapper {

    private PaymentEntityMapper() {
    }

    static PaymentEntity fromDomain(Payment payment) {
        return new PaymentEntity(
                payment.getId(),
                payment.getMoney().amount(),
                payment.getMoney()
                        .currency()
                        .getCurrencyCode(),
                payment.getStatus()
        );
    }

    static Payment toDomain(PaymentEntity entity) {
        Money money = new Money(
                entity.getAmount(),
                Currency.getInstance(entity.getCurrency())
        );

        return Payment.restore(
                entity.getId(),
                money,
                entity.getStatus()
        );
    }
}