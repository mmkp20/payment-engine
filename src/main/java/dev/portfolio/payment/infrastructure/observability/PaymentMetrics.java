package dev.portfolio.payment.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetrics {

    private final Counter successCounter;
    private final Counter retryCounter;
    private final Counter finalFailureCounter;

    public PaymentMetrics(MeterRegistry registry) {
        successCounter = Counter.builder(
                        "payment.processing"
                )
                .description(
                        "Number of payment processing outcomes"
                )
                .tag("outcome", "success")
                .register(registry);

        retryCounter = Counter.builder(
                        "payment.processing"
                )
                .description(
                        "Number of payment processing outcomes"
                )
                .tag("outcome", "retry")
                .register(registry);

        finalFailureCounter = Counter.builder(
                        "payment.processing"
                )
                .description(
                        "Number of payment processing outcomes"
                )
                .tag("outcome", "final_failure")
                .register(registry);
    }

    public void recordSuccess() {
        successCounter.increment();
    }

    public void recordRetry() {
        retryCounter.increment();
    }

    public void recordFinalFailure() {
        finalFailureCounter.increment();
    }
}