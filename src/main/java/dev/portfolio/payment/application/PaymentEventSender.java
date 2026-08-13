package dev.portfolio.payment.application;

public interface PaymentEventSender {

    void send(String payload);
}