package com.teste.bank.fixture;

import com.teste.bank.application.service.NotificationCommand;
import com.teste.bank.domain.model.Notification;
import com.teste.bank.domain.model.NotificationStatus;
import com.teste.bank.domain.model.TransferDirection;
import java.time.LocalDateTime;

public final class NotificationFixture {

    public static final Long NOTIFICATION_ID = 50L;
    public static final String MESSAGE = "Você enviou R$ 30.00 para a conta 2 (transferência 10)";

    private NotificationFixture() {
    }

    public static NotificationCommand debitCommand() {
        return NotificationCommand.builder()
                .accountId(AccountFixture.SOURCE_ID)
                .transferId(TransferFixture.TRANSFER_ID)
                .direction(TransferDirection.DEBIT)
                .message(MESSAGE)
                .build();
    }

    public static Notification notification(NotificationStatus status) {
        return Notification.builder()
                .id(NOTIFICATION_ID)
                .accountId(AccountFixture.SOURCE_ID)
                .transferId(TransferFixture.TRANSFER_ID)
                .direction(TransferDirection.DEBIT)
                .message(MESSAGE)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
