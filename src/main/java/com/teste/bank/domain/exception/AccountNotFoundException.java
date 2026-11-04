package com.teste.bank.domain.exception;

public class AccountNotFoundException extends BusinessException {

    public AccountNotFoundException(Long accountId) {
        super(ErrorCode.ACCOUNT_NOT_FOUND, accountId);
    }
}
