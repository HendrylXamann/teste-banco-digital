package com.teste.bank.application.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.teste.bank.application.notification.NotificationMessage;
import com.teste.bank.application.service.NotificationCommand;
import com.teste.bank.application.service.NotificationService;
import com.teste.bank.domain.model.TransferDirection;
import com.teste.bank.fixture.TransferFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferNotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransferNotificationListener listener;

    @Test
    void shouldNotifyPayerAndReceiver() {
        var event = TransferFixture.completedEvent();
        var amount = event.amount().toPlainString();
        var captor = ArgumentCaptor.forClass(NotificationCommand.class);

        listener.onTransferCompleted(event);

        verify(notificationService, times(2)).send(captor.capture());
        var payer = captor.getAllValues().getFirst();
        var receiver = captor.getAllValues().getLast();
        assertThat(payer.accountId()).isEqualTo(event.fromAccountId());
        assertThat(payer.direction()).isEqualTo(TransferDirection.DEBIT);
        assertThat(payer.message())
                .isEqualTo(NotificationMessage.TRANSFER_SENT.format(amount, event.toAccountId(), event.transferId()));
        assertThat(receiver.accountId()).isEqualTo(event.toAccountId());
        assertThat(receiver.direction()).isEqualTo(TransferDirection.CREDIT);
        assertThat(receiver.message()).isEqualTo(
                NotificationMessage.TRANSFER_RECEIVED.format(amount, event.fromAccountId(), event.transferId()));
        assertThat(captor.getAllValues()).allMatch(command -> command.transferId().equals(event.transferId()));
    }
}
