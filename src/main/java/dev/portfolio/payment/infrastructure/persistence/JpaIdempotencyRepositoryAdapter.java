package dev.portfolio.payment.infrastructure.persistence;

import dev.portfolio.payment.domain.IdempotencyKey;
import dev.portfolio.payment.domain.IdempotencyRepository;
import dev.portfolio.payment.domain.Payment;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JpaIdempotencyRepositoryAdapter
        implements IdempotencyRepository {

    private final SpringDataIdempotencyRepository repository;
    private final SpringDataPaymentRepository paymentRepository;

    public JpaIdempotencyRepositoryAdapter(
            SpringDataIdempotencyRepository repository,
            SpringDataPaymentRepository paymentRepository
    ) {
        this.repository = repository;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Optional<Payment> findByKey(IdempotencyKey key) {
        return repository.findWithPaymentByKey(key.value())
                .map(IdempotencyRecordEntity::getPayment)
                .map(PaymentEntityMapper::toDomain);
    }

    @Override
    public void save(
            IdempotencyKey key,
            Payment payment
    ) {
        PaymentEntity paymentEntity =
                paymentRepository.findById(payment.getId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Payment must be saved before " +
                                                "its idempotency record"
                                )
                        );

        IdempotencyRecordEntity record =
                new IdempotencyRecordEntity(
                        key.value(),
                        paymentEntity
                );

        repository.save(record);
    }
}