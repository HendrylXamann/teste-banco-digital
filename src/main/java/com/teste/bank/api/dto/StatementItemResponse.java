package com.teste.bank.api.dto;

import com.teste.bank.domain.model.TransferDirection;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record StatementItemResponse(
        Long transferId,
        TransferDirection direction,
        Long counterpartAccountId,
        BigDecimal amount,
        LocalDateTime createdAt) {
}
