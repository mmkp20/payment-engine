package dev.portfolio.payment.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MoneyTest {
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    public void validMoneyIsCreated() {
        Money money = new Money(new BigDecimal("10.99"), USD);
        assertThat(money.amount()).isEqualTo(new BigDecimal("10.99"));
        assertThat(money.currency()).isEqualTo(USD);
    }

    @Test
    public void zeroAmountIsRejected() {
        assertThatThrownBy(
            ()-> new Money(new BigDecimal("0.00"), USD)
        ).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("amount must be greater than zero");
    }

    @Test
    public void negativeAmountIsRejected() {
        assertThatThrownBy(
                ()-> new Money(new BigDecimal("-1.00"), USD)
        ).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("amount must be greater than zero");
    }

    @Test
    public void amountWithTooManyDecimalPlacesIsRejected() {
        assertThatThrownBy(
                ()-> new Money(new BigDecimal("10.999"), USD)
        ).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("amount has too many decimal places for USD");
    }

    @Test
    public void nullAmountIsRejected() {
        assertThatThrownBy(() -> new Money(null, USD))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("amount must not be null");
    }

    @Test
    public void nullCurrencyIsRejected() {
        assertThatThrownBy(
                ()-> new Money(new BigDecimal("10.99"), null)
        ).isInstanceOf(NullPointerException.class)
        .hasMessage("currency must not be null");
    }

}
