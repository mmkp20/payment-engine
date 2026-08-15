package dev.portfolio.payment.application;

import dev.portfolio.payment.domain.Payment;
import dev.portfolio.payment.domain.PaymentRepository;
import dev.portfolio.payment.domain.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentProcessor {

    private final PaymentRepository paymentRepository;

    public PaymentProcessor(
            PaymentRepository paymentRepository
    ) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment processPayment(UUID paymentId) {
        Payment payment = paymentRepository
                .findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId)
                );

        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return payment;
        }

        if (payment.getStatus() == PaymentStatus.FAILED) {
            throw new IllegalStateException(
                    "Failed payment cannot be processed again"
            );
        }

        if (payment.getStatus() == PaymentStatus.CREATED) {
            payment.startProcessing();
        }

        payment.markSucceeded();

        return paymentRepository.save(payment);
    }
}