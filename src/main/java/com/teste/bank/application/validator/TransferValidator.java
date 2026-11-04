package com.teste.bank.application.validator;

import com.teste.bank.domain.exception.ErrorCode;
import com.teste.bank.domain.exception.InsufficientBalanceException;
import com.teste.bank.domain.exception.InvalidTransferException;
import com.teste.bank.domain.model.Account;
import java.math.BigDecimal;
import java.util.Objects;

public final class TransferValidator {

    private TransferValidator() {
    }

    public static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransferException(ErrorCode.INVALID_AMOUNT);
        }
    }

    public static void validateDistinctAccounts(Long fromAccountId, Long toAccountId) {
        if (Objects.equals(fromAccountId, toAccountId)) {
            throw new InvalidTransferException(ErrorCode.SAME_ACCOUNT_TRANSFER);
        }
    }

    public static void validateBalance(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(account.getId());
        }
    }
}
