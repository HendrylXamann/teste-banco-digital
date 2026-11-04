package com.teste.bank.application.notification;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum NotificationMessage {

    TRANSFER_SENT("Você enviou R$ %s para a conta %d (transferência %d)"),
    TRANSFER_RECEIVED("Você recebeu R$ %s da conta %d (transferência %d)");

    private final String template;

    public String format(Object... args) {
        return template.formatted(args);
    }
}
