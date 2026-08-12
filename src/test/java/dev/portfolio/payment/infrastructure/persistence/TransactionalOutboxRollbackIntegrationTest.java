package dev.portfolio.payment.infrastructure.persistence;

import dev.portfolio.payment.application.OutboxEventWriter;
import dev.portfolio.payment.application.PaymentService;
import dev.portfolio.payment.domain.IdempotencyKey;
import dev.portfolio.payment.domain.IdempotencyRepository;
import dev.portfolio.payment.domain.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@Testcontainers
@SpringBootTest
class TransactionalOutboxRollbackIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine");

    @MockitoBean
    private OutboxEventWriter outboxEventWriter;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SpringDataPaymentRepository paymentRepository;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Test
    void outboxFailureRollsBackPaymentAndIdempotencyRecord() {
        IdempotencyKey key =
                new IdempotencyKey("rollback-request");

        long paymentCountBefore =
                paymentRepository.count();

        doThrow(new RuntimeException("Outbox unavailable"))
                .when(outboxEventWriter)
                .save(any());

        assertThatThrownBy(() ->
                paymentService.createPayment(
                        key,
                        new Money(
                                new BigDecimal("80.00"),
                                Currency.getInstance("USD")
                        )
                )
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Outbox unavailable");

        assertThat(paymentRepository.count())
                .isEqualTo(paymentCountBefore);

        assertThat(idempotencyRepository.findByKey(key))
                .isEmpty();
    }
}