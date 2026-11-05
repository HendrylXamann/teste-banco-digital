package com.teste.bank.application.listener;

import com.teste.bank.application.event.TransferCompletedEvent;
import com.teste.bank.application.notification.NotificationMessage;
import com.teste.bank.application.service.NotificationCommand;
import com.teste.bank.application.service.NotificationService;
import com.teste.bank.domain.model.TransferDirection;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TransferNotificationListener {

    private final NotificationService notificationService;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferCompleted(TransferCompletedEvent event) {
        var amount = event.amount().toPlainString();
        send(event, event.fromAccountId(), TransferDirection.DEBIT,
                NotificationMessage.TRANSFER_SENT.format(amount, event.toAccountId(), event.transferId()));
        send(event, event.toAccountId(), TransferDirection.CREDIT,
                NotificationMessage.TRANSFER_RECEIVED.format(amount, event.fromAccountId(), event.transferId()));
    }

    private void send(TransferCompletedEvent event, Long accountId, TransferDirection direction, String message) {
        notificationService.send(NotificationCommand.builder()
                .accountId(accountId)
                .transferId(event.transferId())
                .direction(direction)
                .message(message)
                .build());
    }
}
