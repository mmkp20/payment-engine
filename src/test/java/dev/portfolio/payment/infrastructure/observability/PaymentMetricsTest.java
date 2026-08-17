package dev.portfolio.payment.infrastructure.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMetricsTest {

    @Test
    void processingOutcomesAreRecorded() {
        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();

        PaymentMetrics metrics =
                new PaymentMetrics(registry);

        metrics.recordSuccess();
        metrics.recordRetry();
        metrics.recordFinalFailure();

        assertThat(
                registry.get("payment.processing")
                        .tag("outcome", "success")
                        .counter()
                        .count()
        ).isEqualTo(1.0);

        assertThat(
                registry.get("payment.processing")
                        .tag("outcome", "retry")
                        .counter()
                        .count()
        ).isEqualTo(1.0);

        assertThat(
                registry.get("payment.processing")
                        .tag("outcome", "final_failure")
                        .counter()
                        .count()
        ).isEqualTo(1.0);
    }
}