package com.teste.bank.application.listener;

import com.teste.bank.application.event.TransferCompletedEvent;
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
        notificationGateway.notify(event.fromAccountId(), debitMessage(event));
        notificationGateway.notify(event.toAccountId(), creditMessage(event));
    }

    private String debitMessage(TransferCompletedEvent event) {
        return "Você enviou R$ %s para a conta %d (transferência %d)"
                .formatted(event.amount().toPlainString(), event.toAccountId(), event.transferId());
    }

    private String creditMessage(TransferCompletedEvent event) {
        return "Você recebeu R$ %s da conta %d (transferência %d)"
                .formatted(event.amount().toPlainString(), event.fromAccountId(), event.transferId());
    }
}
