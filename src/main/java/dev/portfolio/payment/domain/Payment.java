package dev.portfolio.payment.domain;

import java.util.Objects;
import java.util.UUID;

public class Payment {
    private final UUID id;
    private final Money money;
    private PaymentStatus status;

    private Payment(UUID id, Money money) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.money = Objects.requireNonNull(money, "money must not be null");
        this.status = PaymentStatus.CREATED;
    }

    public static Payment create(UUID id, Money money) {
        return new Payment(id, money);
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
}
