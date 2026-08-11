package dev.portfolio.payment.infrastructure.persistence;

import dev.portfolio.payment.domain.IdempotencyKey;
import dev.portfolio.payment.domain.IdempotencyRepository;
import dev.portfolio.payment.domain.Money;
import dev.portfolio.payment.domain.Payment;
import dev.portfolio.payment.domain.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@Transactional
class PostgreSQLRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Test
    void paymentIsPersistedAndRetrieved() {
        UUID paymentId = UUID.randomUUID();

        Payment payment = Payment.create(
                paymentId,
                new Money(
                        new BigDecimal("49.99"),
                        Currency.getInstance("USD")
                )
        );

        paymentRepository.save(payment);

        Payment foundPayment = paymentRepository
                .findById(paymentId)
                .orElseThrow();

        assertThat(foundPayment.getId())
                .isEqualTo(paymentId);
        assertThat(foundPayment.getMoney())
                .isEqualTo(payment.getMoney());
        assertThat(foundPayment.getStatus())
                .isEqualTo(payment.getStatus());
    }

    @Test
    void idempotencyRecordIsPersistedAndRetrieved() {
        UUID paymentId = UUID.randomUUID();

        Payment payment = Payment.create(
                paymentId,
                new Money(
                        new BigDecimal("75.00"),
                        Currency.getInstance("USD")
                )
        );

        IdempotencyKey key =
                new IdempotencyKey("integration-request-1");

        paymentRepository.save(payment);
        idempotencyRepository.save(key, payment);

        Payment foundPayment = idempotencyRepository
                .findByKey(key)
                .orElseThrow();

        assertThat(foundPayment.getId())
                .isEqualTo(paymentId);
        assertThat(foundPayment.getMoney())
                .isEqualTo(payment.getMoney());
    }
}