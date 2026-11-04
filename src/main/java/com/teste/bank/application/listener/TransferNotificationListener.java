package com.teste.bank.application.listener;

import com.teste.bank.application.event.TransferCompletedEvent;
import com.teste.bank.application.notification.NotificationMessage;
import com.teste.bank.domain.gateway.NotificationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TransferNotificationListener {

    private final NotificationGateway notificationGateway;

    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransferCompleted(TransferCompletedEvent event) {
        String amount = event.amount().toPlainString();
        notificationGateway.notify(event.fromAccountId(),
                NotificationMessage.TRANSFER_SENT.format(amount, event.toAccountId(), event.transferId()));
        notificationGateway.notify(event.toAccountId(),
                NotificationMessage.TRANSFER_RECEIVED.format(amount, event.fromAccountId(), event.transferId()));
    }
}
