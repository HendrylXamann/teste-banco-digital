package com.teste.bank.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.teste.bank.domain.gateway.NotificationGateway;
import com.teste.bank.domain.model.Notification;
import com.teste.bank.domain.model.NotificationStatus;
import com.teste.bank.domain.model.TransferDirection;
import com.teste.bank.domain.repository.NotificationRepository;
import com.teste.bank.fixture.AccountFixture;
import com.teste.bank.fixture.NotificationFixture;
import com.teste.bank.fixture.TransferFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationGateway notificationGateway;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void returnSavedEntity() {
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0, Notification.class));
    }

    @Test
    void shouldRecordSentNotificationWhenGatewayDelivers() {
        when(notificationGateway.notify(AccountFixture.SOURCE_ID, NotificationFixture.MESSAGE)).thenReturn(true);

        var notification = notificationService.send(NotificationFixture.debitCommand());

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getAccountId()).isEqualTo(AccountFixture.SOURCE_ID);
        assertThat(notification.getTransferId()).isEqualTo(TransferFixture.TRANSFER_ID);
        assertThat(notification.getDirection()).isEqualTo(TransferDirection.DEBIT);
        assertThat(notification.getMessage()).isEqualTo(NotificationFixture.MESSAGE);
        assertThat(notification.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldRecordFailedNotificationWhenGatewayGivesUp() {
        when(notificationGateway.notify(AccountFixture.SOURCE_ID, NotificationFixture.MESSAGE)).thenReturn(false);

        var notification = notificationService.send(NotificationFixture.debitCommand());

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
    }
}
