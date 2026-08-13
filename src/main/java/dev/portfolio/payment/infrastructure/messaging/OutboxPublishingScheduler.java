package dev.portfolio.payment.infrastructure.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "app.outbox.publisher.enabled",
        havingValue = "true"
)
public class OutboxPublishingScheduler {

    private final OutboxPublisher outboxPublisher;

    public OutboxPublishingScheduler(
            OutboxPublisher outboxPublisher
    ) {
        this.outboxPublisher = outboxPublisher;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.outbox.publisher.delay-ms}"
    )
    public void publishPendingEvents() {
        outboxPublisher.publishPendingEvents();
    }
}