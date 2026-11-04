package com.teste.bank.domain.exception;

public class InsufficientBalanceException extends BusinessException {

    public InsufficientBalanceException(Long accountId) {
        super("Saldo insuficiente na conta: " + accountId);
    }
}
