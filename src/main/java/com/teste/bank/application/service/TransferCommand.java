package com.teste.bank.application.service;

import java.math.BigDecimal;

public record TransferCommand(Long fromAccountId, Long toAccountId, BigDecimal amount, String idempotencyKey) {
}
