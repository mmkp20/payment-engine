package dev.portfolio.payment.infrastructure.messaging;

import dev.portfolio.payment.application.PaymentEventSender;
import dev.portfolio.payment.domain.OutboxStatus;
import dev.portfolio.payment.infrastructure.persistence.OutboxEventEntity;
import dev.portfolio.payment.infrastructure.persistence.SpringDataOutboxRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherTest {

    @Test
    void successfulEventIsMarkedPublished() {
        SpringDataOutboxRepository repository =
                mock(SpringDataOutboxRepository.class);

        PaymentEventSender sender =
                mock(PaymentEventSender.class);

        OutboxEventEntity event = new OutboxEventEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PAYMENT_CREATED",
                "{\"paymentId\":\"123\"}"
        );

        when(repository
                .findTop10ByStatusOrderByCreatedAtAsc(
                        OutboxStatus.PENDING
                ))
                .thenReturn(List.of(event));

        OutboxPublisher publisher =
                new OutboxPublisher(repository, sender);

        int publishedCount =
                publisher.publishPendingEvents();

        verify(sender).send(event.getPayload());

        assertThat(publishedCount).isEqualTo(1);
        assertThat(event.getStatus())
                .isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getAttempts()).isZero();
    }

    @Test
    void failedEventRemainsPendingAndRecordsAttempt() {
        SpringDataOutboxRepository repository =
                mock(SpringDataOutboxRepository.class);

        PaymentEventSender sender =
                mock(PaymentEventSender.class);

        OutboxEventEntity event = new OutboxEventEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PAYMENT_CREATED",
                "{\"paymentId\":\"123\"}"
        );

        when(repository
                .findTop10ByStatusOrderByCreatedAtAsc(
                        OutboxStatus.PENDING
                ))
                .thenReturn(List.of(event));

        doThrow(new RuntimeException("SQS unavailable"))
                .when(sender)
                .send(anyString());

        OutboxPublisher publisher =
                new OutboxPublisher(repository, sender);

        int publishedCount =
                publisher.publishPendingEvents();

        assertThat(publishedCount).isZero();
        assertThat(event.getStatus())
                .isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getPublishedAt()).isNull();
    }
}