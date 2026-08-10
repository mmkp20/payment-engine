package dev.portfolio.payment.infrastructure.persistence;

import dev.portfolio.payment.domain.IdempotencyKey;
import dev.portfolio.payment.domain.IdempotencyRepository;
import dev.portfolio.payment.domain.Payment;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("in-memory")
public class InMemoryIdempotencyRepository implements IdempotencyRepository {
    private final Map<IdempotencyKey, Payment> paymentsByKey =
            new ConcurrentHashMap<>();

    @Override
    public Optional<Payment> findByKey(IdempotencyKey key) {
        return Optional.ofNullable(paymentsByKey.get(key));
    }

    @Override
    public void save(IdempotencyKey key, Payment payment) {
        paymentsByKey.put(key, payment);
    }
}
