package com.teste.bank.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record TransferResponse(
        Long id,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        LocalDateTime createdAt) {
}
