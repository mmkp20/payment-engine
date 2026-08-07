package dev.portfolio.payment.api;

import dev.portfolio.payment.application.PaymentService;
import dev.portfolio.payment.domain.IdempotencyKey;
import dev.portfolio.payment.domain.Money;
import dev.portfolio.payment.domain.Payment;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Currency;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createPayment(@RequestHeader("Idempotency-key") String idempotencyKeyValue,
                                        @Valid @RequestBody CreatePaymentRequest request) {
        Money money = new Money(request.amount(),
                Currency.getInstance(request.currency()));

        Payment payment = paymentService.createPayment(new IdempotencyKey(idempotencyKeyValue),money);
        return PaymentResponse.from(payment);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable UUID paymentId) {
        Payment payment = paymentService.getPayment(paymentId);
        return PaymentResponse.from(payment);
    }
}
