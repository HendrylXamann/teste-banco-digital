package com.teste.bank.domain.exception;

public class AccountNotFoundException extends BusinessException {

    public AccountNotFoundException(Long accountId) {
        super("Conta não encontrada: " + accountId);
    }
}
