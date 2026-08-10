package dev.portfolio.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataIdempotencyRepository
        extends JpaRepository<IdempotencyRecordEntity, String> {

    @Query("""
            SELECT record
            FROM IdempotencyRecordEntity record
            JOIN FETCH record.payment
            WHERE record.idempotencyKey = :key
            """)
    Optional<IdempotencyRecordEntity> findWithPaymentByKey(
            @Param("key") String key
    );
}