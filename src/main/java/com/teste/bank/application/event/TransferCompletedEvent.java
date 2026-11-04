package com.teste.bank.application.event;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record TransferCompletedEvent(Long transferId, Long fromAccountId, Long toAccountId, BigDecimal amount) {
}
