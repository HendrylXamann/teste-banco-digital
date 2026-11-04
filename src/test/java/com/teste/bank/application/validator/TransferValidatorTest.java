package com.teste.bank.application.validator;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teste.bank.domain.exception.InsufficientBalanceException;
import com.teste.bank.domain.exception.InvalidTransferException;
import com.teste.bank.domain.model.Account;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransferValidatorTest {

    @Test
    void shouldThrowWhenAccountsAreTheSame() {
        assertThatThrownBy(() -> TransferValidator.validateDistinctAccounts(1L, 1L))
                .isInstanceOf(InvalidTransferException.class);
    }

    @Test
    void shouldPassWhenAccountsAreDifferent() {
        assertThatCode(() -> TransferValidator.validateDistinctAccounts(1L, 2L)).doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenAmountIsZero() {
        assertThatThrownBy(() -> TransferValidator.validateAmount(BigDecimal.ZERO))
                .isInstanceOf(InvalidTransferException.class);
    }

    @Test
    void shouldThrowWhenAmountIsNegative() {
        assertThatThrownBy(() -> TransferValidator.validateAmount(new BigDecimal("-1.00")))
                .isInstanceOf(InvalidTransferException.class);
    }

    @Test
    void shouldThrowWhenAmountIsNull() {
        assertThatThrownBy(() -> TransferValidator.validateAmount(null))
                .isInstanceOf(InvalidTransferException.class);
    }

    @Test
    void shouldThrowWhenBalanceIsInsufficient() {
        Account account = accountWithBalance("50.00");

        assertThatThrownBy(() -> TransferValidator.validateBalance(account, new BigDecimal("50.01")))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void shouldPassWhenBalanceIsExactlyTheAmount() {
        Account account = accountWithBalance("50.00");

        assertThatCode(() -> TransferValidator.validateBalance(account, new BigDecimal("50.00")))
                .doesNotThrowAnyException();
    }

    private Account accountWithBalance(String balance) {
        Account account = new Account();
        account.setId(1L);
        account.setBalance(new BigDecimal(balance));
        return account;
    }
}
