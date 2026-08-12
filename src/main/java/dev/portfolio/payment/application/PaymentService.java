package dev.portfolio.payment.application;

import dev.portfolio.payment.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final OutboxEventWriter outboxEventWriter;

    public PaymentService(PaymentRepository paymentRepository, IdempotencyRepository idempotencyRepository, OutboxEventWriter outboxEventWriter) {
        this.paymentRepository = paymentRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.outboxEventWriter = outboxEventWriter;
    }

    @Transactional
    public Payment createPayment(IdempotencyKey idempotencyKey, Money money){
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(money, "money must not be null");

        return idempotencyRepository.findByKey(idempotencyKey)
                .map(existingPayment -> {
                    if(!existingPayment.getMoney().equals(money)){
                        throw new IdempotencyConflictException(idempotencyKey);
                    }
                    return existingPayment;
                }).orElseGet(() -> {
                    Payment payment = Payment.create(
                            UUID.randomUUID(),
                            money
                    );

                    Payment savedPayment =
                            paymentRepository.save(payment);

                    idempotencyRepository.save(
                            idempotencyKey,
                            savedPayment
                    );

                    outboxEventWriter.save(
                            PaymentCreatedEvent.from(savedPayment)
                    );

                    return savedPayment;
                });
    }

    public Payment getPayment(UUID paymentId){
        Objects.requireNonNull(paymentId, "paymentId must not be null");

        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
