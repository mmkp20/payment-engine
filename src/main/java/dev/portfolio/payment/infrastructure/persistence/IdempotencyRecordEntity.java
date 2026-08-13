package dev.portfolio.payment.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecordEntity {

    @Id
    @Column(name = "idempotency_key",
            nullable = false,
            length = 255)
    private String idempotencyKey;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id",
            nullable = false,
            unique = true)
    private PaymentEntity payment;

    @Column(name = "created_at",
            nullable = false,
            updatable = false)
    private Instant createdAt;

    protected IdempotencyRecordEntity() {
        // Required by JPA
    }

    public IdempotencyRecordEntity(String idempotencyKey,PaymentEntity payment) {
        this.idempotencyKey = idempotencyKey;
        this.payment = payment;
        this.createdAt = Instant.now();
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public PaymentEntity getPayment() {
        return payment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}