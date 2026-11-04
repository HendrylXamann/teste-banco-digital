package com.teste.bank.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(Long id, String name, BigDecimal balance, LocalDateTime createdAt) {
}
