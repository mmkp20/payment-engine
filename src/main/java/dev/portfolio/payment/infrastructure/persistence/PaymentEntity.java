package dev.portfolio.payment.infrastructure.persistence;

import dev.portfolio.payment.domain.PaymentStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    private UUID id;

    @Column(nullable = false,
            precision = 19,
            scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at",
            nullable = false,
            updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentEntity() {
        // Required by JPA
    }

    public PaymentEntity(
            UUID id,
            BigDecimal amount,
            String currency,
            PaymentStatus status) {
        this.id = id;
        this.amount = amount;
        this.currency = currency;
        this.status = status;

        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateStatus(PaymentStatus status) {
        this.status = Objects.requireNonNull(
                status,
                "status must not be null"
        );
    }
}