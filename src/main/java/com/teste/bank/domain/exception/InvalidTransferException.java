package com.teste.bank.domain.exception;

public class InvalidTransferException extends BusinessException {

    public InvalidTransferException(String message) {
        super(message);
    }
}
