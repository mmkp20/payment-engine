package dev.portfolio.payment.infrastructure.persistence;

import dev.portfolio.payment.application.PaymentProcessor;
import dev.portfolio.payment.application.PaymentService;
import dev.portfolio.payment.domain.*;
import jakarta.persistence.EntityManager;
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

    @Autowired
    private SpringDataOutboxRepository outboxRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentProcessor paymentProcessor;

    @Autowired
    private EntityManager entityManager;

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

    @Test
    void outboxEventIsPersistedAndRetrieved() {
        UUID paymentId = UUID.randomUUID();

        Payment payment = Payment.create(
                paymentId,
                new Money(
                        new BigDecimal("30.00"),
                        Currency.getInstance("USD")
                )
        );

        paymentRepository.save(payment);

        UUID eventId = UUID.randomUUID();

        OutboxEventEntity event = new OutboxEventEntity(
                eventId,
                paymentId,
                "PAYMENT_CREATED",
                """
                {
                  "paymentId": "%s",
                  "amount": 30.00,
                  "currency": "USD"
                }
                """.formatted(paymentId)
        );

        outboxRepository.save(event);

        OutboxEventEntity foundEvent = outboxRepository
                .findById(eventId)
                .orElseThrow();

        assertThat(foundEvent.getAggregateId())
                .isEqualTo(paymentId);
        assertThat(foundEvent.getEventType())
                .isEqualTo("PAYMENT_CREATED");
        assertThat(foundEvent.getStatus())
                .isEqualTo(OutboxStatus.PENDING);
        assertThat(foundEvent.getAttempts()).isZero();
        assertThat(foundEvent.getPublishedAt()).isNull();
    }

    @Test
    void paymentCreationPersistsTransactionalOutboxEvent() {
        IdempotencyKey key =
                new IdempotencyKey("transactional-outbox-request");

        Payment payment = paymentService.createPayment(
                key,
                new Money(
                        new BigDecimal("65.00"),
                        Currency.getInstance("USD")
                )
        );

        assertThat(paymentRepository.findById(payment.getId()))
                .isPresent();

        assertThat(idempotencyRepository.findByKey(key))
                .isPresent();

        var outboxEvents = outboxRepository
                .findAllByAggregateId(payment.getId());

        assertThat(outboxEvents).hasSize(1);

        OutboxEventEntity event = outboxEvents.get(0);

        assertThat(event.getEventType()).isEqualTo("PAYMENT_CREATED");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getPayload()).contains(payment.getId().toString())
                                        .contains("\"amount\": 65.00")
                                        .contains("\"currency\": \"USD\"");
    }

    @Test
    void existingPaymentStatusIsUpdatedInPostgreSQL() {
        UUID paymentId = UUID.randomUUID();

        Payment payment = Payment.create(
                paymentId,
                new Money(
                        new BigDecimal("80.00"),
                        Currency.getInstance("USD")
                )
        );

        paymentRepository.save(payment);

        Payment processedPayment =
                paymentProcessor.processPayment(paymentId);

        entityManager.flush();
        entityManager.clear();

        Payment reloadedPayment = paymentRepository
                .findById(paymentId)
                .orElseThrow();

        assertThat(processedPayment.getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);

        assertThat(reloadedPayment.getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);
    }
}