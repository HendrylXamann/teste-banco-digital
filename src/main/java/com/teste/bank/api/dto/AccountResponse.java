package com.teste.bank.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AccountResponse(Long id, String name, BigDecimal balance, LocalDateTime createdAt) {
}
