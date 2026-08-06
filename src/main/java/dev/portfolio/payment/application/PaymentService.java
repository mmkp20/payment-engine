package dev.portfolio.payment.application;

import dev.portfolio.payment.domain.Money;
import dev.portfolio.payment.domain.Payment;
import dev.portfolio.payment.domain.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment createPayment(Money money){
        Objects.requireNonNull(money, "money must not be null");
        Payment payment = Payment.create(
                UUID.randomUUID(),
                money
        );
        return paymentRepository.save(payment);
    }

    public Payment getPayment(UUID paymentId){
        Objects.requireNonNull(paymentId, "paymentId must not be null");

        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
