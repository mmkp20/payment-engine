package dev.portfolio.payment.infrastructure.persistence;

import dev.portfolio.payment.application.OutboxEventWriter;
import dev.portfolio.payment.application.PaymentCreatedEvent;
import org.springframework.stereotype.Component;

@Component
public class JpaOutboxEventWriter implements OutboxEventWriter {

    private final SpringDataOutboxRepository repository;

    public JpaOutboxEventWriter(
            SpringDataOutboxRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public void save(PaymentCreatedEvent event) {
        String payload = """
                {
                  "eventId": "%s",
                  "paymentId": "%s",
                  "amount": %s,
                  "currency": "%s",
                  "occurredAt": "%s"
                }
                """.formatted(
                event.eventId(),
                event.paymentId(),
                event.amount().toPlainString(),
                event.currency(),
                event.occurredAt()
        );

        OutboxEventEntity entity = new OutboxEventEntity(
                event.eventId(),
                event.paymentId(),
                "PAYMENT_CREATED",
                payload
        );

        repository.save(entity);
    }
}