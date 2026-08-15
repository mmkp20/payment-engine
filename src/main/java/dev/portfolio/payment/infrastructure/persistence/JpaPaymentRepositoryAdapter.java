package dev.portfolio.payment.infrastructure.persistence;

import dev.portfolio.payment.domain.Payment;
import dev.portfolio.payment.domain.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaPaymentRepositoryAdapter
        implements PaymentRepository {

    private final SpringDataPaymentRepository repository;

    public JpaPaymentRepositoryAdapter(SpringDataPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Payment save(Payment payment) {
        return repository.findById(payment.getId())
                .map(existingEntity -> {
                    existingEntity.updateStatus(payment.getStatus());

                    PaymentEntity savedEntity =
                            repository.save(existingEntity);

                    return PaymentEntityMapper.toDomain(savedEntity);
                })
                .orElseGet(() -> {
                    PaymentEntity newEntity =
                            PaymentEntityMapper.fromDomain(payment);

                    PaymentEntity savedEntity =
                            repository.save(newEntity);

                    return PaymentEntityMapper.toDomain(savedEntity);
                });
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return repository.findById(id).map(PaymentEntityMapper::toDomain);
    }
}