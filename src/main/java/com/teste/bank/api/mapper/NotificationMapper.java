package com.teste.bank.api.mapper;

import com.teste.bank.api.dto.NotificationResponse;
import com.teste.bank.domain.model.Notification;

public final class NotificationMapper {

    private NotificationMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .transferId(notification.getTransferId())
                .direction(notification.getDirection())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
