package com.teste.bank.domain.exception;

public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(Long accountId) {
        super(ErrorCode.INSUFFICIENT_BALANCE, accountId);
    }
}
