package com.teste.bank.application.listener;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.teste.bank.application.event.TransferCompletedEvent;
import com.teste.bank.domain.gateway.NotificationGateway;
import java.math.BigDecimal;
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
        TransferCompletedEvent event = new TransferCompletedEvent(7L, 1L, 2L, new BigDecimal("15.00"));

        listener.onTransferCompleted(event);

        verify(notificationGateway).notify(eq(1L), contains("enviou R$ 15.00"));
        verify(notificationGateway).notify(eq(2L), contains("recebeu R$ 15.00"));
    }
}
