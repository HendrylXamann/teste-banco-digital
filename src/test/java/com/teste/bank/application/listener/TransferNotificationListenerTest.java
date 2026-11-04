package com.teste.bank.application.listener;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.teste.bank.application.notification.NotificationMessage;
import com.teste.bank.domain.gateway.NotificationGateway;
import com.teste.bank.fixture.TransferFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferNotificationListenerTest {

    @Mock
    private NotificationGateway notificationGateway;

    @InjectMocks
    private TransferNotificationListener listener;

    @Test
    void shouldNotifyPayerAndReceiver() {
        var event = TransferFixture.completedEvent();
        var amount = event.amount().toPlainString();

        listener.onTransferCompleted(event);

        verify(notificationGateway).notify(event.fromAccountId(),
                NotificationMessage.TRANSFER_SENT.format(amount, event.toAccountId(), event.transferId()));
        verify(notificationGateway).notify(event.toAccountId(),
                NotificationMessage.TRANSFER_RECEIVED.format(amount, event.fromAccountId(), event.transferId()));
        verifyNoMoreInteractions(notificationGateway);
    }
}
