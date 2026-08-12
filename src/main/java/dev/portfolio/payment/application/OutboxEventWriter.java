package dev.portfolio.payment.application;

public interface OutboxEventWriter {

    void save(PaymentCreatedEvent event);
}