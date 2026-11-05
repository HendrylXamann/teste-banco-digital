package com.teste.bank.application.service;

import com.teste.bank.domain.model.TransferDirection;
import lombok.Builder;

@Builder
public record NotificationCommand(Long accountId, Long transferId, TransferDirection direction, String message) {
}
