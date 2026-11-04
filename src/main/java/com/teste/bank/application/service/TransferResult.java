package com.teste.bank.application.service;

import com.teste.bank.domain.model.Transfer;
import lombok.Builder;

@Builder
public record TransferResult(Transfer transfer, boolean created) {
}
