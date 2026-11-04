package com.teste.bank.application.service;

import com.teste.bank.domain.model.TransferDirection;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StatementEntry(
        Long transferId,
        TransferDirection direction,
        Long counterpartAccountId,
        BigDecimal amount,
        LocalDateTime createdAt) {
}
