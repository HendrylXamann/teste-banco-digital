package com.teste.bank.application.service.impl;

import com.teste.bank.application.service.NotificationCommand;
import com.teste.bank.application.service.NotificationService;
import com.teste.bank.domain.gateway.NotificationGateway;
import com.teste.bank.domain.model.Notification;
import com.teste.bank.domain.model.NotificationStatus;
import com.teste.bank.domain.repository.NotificationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationGateway notificationGateway;
    private final NotificationRepository notificationRepository;

    @Override
    public Notification send(NotificationCommand command) {
        var delivered = notificationGateway.notify(command.accountId(), command.message());
        var notification = Notification.builder()
                .accountId(command.accountId())
                .transferId(command.transferId())
                .direction(command.direction())
                .message(command.message())
                .status(delivered ? NotificationStatus.SENT : NotificationStatus.FAILED)
                .createdAt(LocalDateTime.now())
                .build();
        return notificationRepository.save(notification);
    }
}
