package com.teste.bank.api.dto;

import com.teste.bank.domain.model.NotificationStatus;
import com.teste.bank.domain.model.TransferDirection;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record NotificationResponse(
        Long id,
        Long transferId,
        TransferDirection direction,
        String message,
        NotificationStatus status,
        LocalDateTime createdAt) {
}
