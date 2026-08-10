package dev.portfolio.payment.domain;

import java.util.Objects;
import java.util.UUID;

public class Payment {
    private final UUID id;
    private final Money money;
    private PaymentStatus status;

    private Payment(UUID id, Money money, PaymentStatus status) {
        this.id = Objects.requireNonNull(
                id,
                "id must not be null"
        );
        this.money = Objects.requireNonNull(
                money,
                "money must not be null"
        );
        this.status = Objects.requireNonNull(
                status,
                "status must not be null"
        );
    }

    public UUID getId() {
        return id;
    }
    public Money getMoney() {
        return money;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void startProcessing(){
        requireStatus(PaymentStatus.CREATED);
        status = PaymentStatus.PROCESSING;
    }

    public void markSucceeded(){
        requireStatus(PaymentStatus.PROCESSING);
        status = PaymentStatus.SUCCEEDED;
    }

    public void markFailed(){
        requireStatus(PaymentStatus.PROCESSING);
        status = PaymentStatus.FAILED;
    }

    private void requireStatus(PaymentStatus expectedStatus){
        if(status != expectedStatus){
            throw new IllegalStateException("Payment must be "+ expectedStatus+" but was "+ status);
        }
    }

    public static Payment create(UUID id, Money money) {
        return new Payment(id, money, PaymentStatus.CREATED);
    }

    public static Payment restore(UUID id, Money money, PaymentStatus status) {
        return new Payment(id, money, status);
    }
}
