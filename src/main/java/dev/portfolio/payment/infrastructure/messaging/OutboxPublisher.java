package dev.portfolio.payment.infrastructure.messaging;

import dev.portfolio.payment.application.PaymentEventSender;
import dev.portfolio.payment.domain.OutboxStatus;
import dev.portfolio.payment.infrastructure.persistence.OutboxEventEntity;
import dev.portfolio.payment.infrastructure.persistence.SpringDataOutboxRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxPublisher {

    private final SpringDataOutboxRepository repository;
    private final PaymentEventSender eventSender;

    public OutboxPublisher(SpringDataOutboxRepository repository,PaymentEventSender eventSender) {
        this.repository = repository;
        this.eventSender = eventSender;
    }

    @Transactional
    public int publishPendingEvents() {
        List<OutboxEventEntity> events = repository.findTop10ByStatusOrderByCreatedAtAsc(
                                OutboxStatus.PENDING);

        int publishedCount = 0;

        for (OutboxEventEntity event : events) {
            try {
                eventSender.send(event.getPayload());
                event.markPublished();
                publishedCount++;
            } catch (RuntimeException exception) {
                event.recordFailedAttempt();
            }
        }

        return publishedCount;
    }
}