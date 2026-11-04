package com.teste.bank.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        Long id,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        LocalDateTime createdAt) {
}
