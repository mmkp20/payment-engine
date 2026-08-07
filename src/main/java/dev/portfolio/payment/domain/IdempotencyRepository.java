package dev.portfolio.payment.domain;

import java.util.Optional;

public interface IdempotencyRepository {

    Optional<Payment> findByKey(IdempotencyKey key);
    void save(IdempotencyKey key, Payment payment);
}
