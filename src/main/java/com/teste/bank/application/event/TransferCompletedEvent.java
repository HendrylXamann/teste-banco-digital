package com.teste.bank.application.event;

import java.math.BigDecimal;

public record TransferCompletedEvent(Long transferId, Long fromAccountId, Long toAccountId, BigDecimal amount) {
}
