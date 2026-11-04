package com.teste.bank.application.service;

import com.teste.bank.domain.model.Transfer;

public record TransferResult(Transfer transfer, boolean created) {
}
