package dev.portfolio.payment.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Pattern;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class CreatePaymentRequestTest {

    private static final Validator VALIDATOR =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void validRequestHasNoViolations() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("10.99"), "USD");

        Set<ConstraintViolation<CreatePaymentRequest>> violations =
                VALIDATOR.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void nullAmountIsRejected() {
        CreatePaymentRequest request = new CreatePaymentRequest(null, "USD");
        Set<ConstraintViolation<CreatePaymentRequest>> violations =
                VALIDATOR.validate(request);
        assertThat(violations).anyMatch(
                violation -> violation.getMessage().equals("amount is required"));
    }

    @Test
    void zeroAmountIsRejected() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("0.00"), "USD");

        Set<ConstraintViolation<CreatePaymentRequest>> violations =
                VALIDATOR.validate(request);

        assertThat(violations).anyMatch(
                violation ->
                        violation.getMessage().equals("amount must be greater than zero")
        );
    }

    @Test
    void blankCurrencyIsRejected() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("10.99"), "");

        Set<ConstraintViolation<CreatePaymentRequest>> violations =
                VALIDATOR.validate(request);

        assertThat(violations).anyMatch(
                violation -> violation.getMessage().equals("currency is required")
        );
    }

    @Test
    void lowercaseCurrencyIsRejected() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("10.99"), "usd"
        );

        Set<ConstraintViolation<CreatePaymentRequest>> violations=
                VALIDATOR.validate(request);

        assertThat(violations).anyMatch(
                violation -> violation.getMessage().equals("currency must be a 3-letter uppercase code")
        );
    }

//    @Test
//    void currencyHasPatternConstraint(){
//        var property = VALIDATOR.getConstraintsForClass(CreatePaymentRequest.class)
//                .getConstraintsForProperty("currency");
//
//        assertThat(property).isNotNull();
//        assertThat(property.getConstraintDescriptors())
//                .anyMatch(descriptor ->
//                        descriptor.getAnnotation().annotationType().equals(Pattern.class));
//    }
}
