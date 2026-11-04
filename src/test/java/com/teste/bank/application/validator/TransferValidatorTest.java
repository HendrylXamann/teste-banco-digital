package com.teste.bank.application.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teste.bank.domain.exception.ErrorCode;
import com.teste.bank.domain.exception.InsufficientBalanceException;
import com.teste.bank.domain.exception.InvalidTransferException;
import com.teste.bank.fixture.AccountFixture;
import com.teste.bank.fixture.Amounts;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class TransferValidatorTest {

    private static final BigDecimal ONE_CENT = new BigDecimal("0.01");

    @Test
    void shouldThrowWhenAccountsAreTheSame() {
        assertThatThrownBy(() -> TransferValidator.validateDistinctAccounts(
                AccountFixture.SOURCE_ID, AccountFixture.SOURCE_ID))
                .isInstanceOf(InvalidTransferException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SAME_ACCOUNT_TRANSFER);
    }

    @Test
    void shouldPassWhenAccountsAreDifferent() {
        assertThatCode(() -> TransferValidator.validateDistinctAccounts(
                AccountFixture.SOURCE_ID, AccountFixture.DESTINATION_ID)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "0.00", "-0.01", "-100.00"})
    void shouldThrowWhenAmountIsNotPositive(String amount) {
        var value = amount == null ? null : new BigDecimal(amount);

        assertThatThrownBy(() -> TransferValidator.validateAmount(value))
                .isInstanceOf(InvalidTransferException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AMOUNT);
    }

    @Test
    void shouldPassWhenAmountIsOneCent() {
        assertThatCode(() -> TransferValidator.validateAmount(ONE_CENT)).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenBalanceIsOneCentShort() {
        var account = AccountFixture.source(Amounts.TRANSFER);

        assertThatThrownBy(() -> TransferValidator.validateBalance(account, Amounts.TRANSFER.add(ONE_CENT)))
                .isInstanceOf(InsufficientBalanceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    void shouldPassWhenBalanceIsExactlyTheAmount() {
        var account = AccountFixture.source(Amounts.TRANSFER);

        assertThatCode(() -> TransferValidator.validateBalance(account, Amounts.TRANSFER))
                .doesNotThrowAnyException();
    }
}
