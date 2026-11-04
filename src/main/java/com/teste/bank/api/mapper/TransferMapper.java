package com.teste.bank.api.mapper;

import com.teste.bank.api.dto.StatementItemResponse;
import com.teste.bank.api.dto.TransferRequest;
import com.teste.bank.api.dto.TransferResponse;
import com.teste.bank.application.service.StatementEntry;
import com.teste.bank.application.service.TransferCommand;
import com.teste.bank.domain.model.Transfer;

public final class TransferMapper {

    private TransferMapper() {
    }

    public static TransferCommand toCommand(TransferRequest request, String idempotencyKey) {
        return new TransferCommand(request.fromAccountId(), request.toAccountId(), request.amount(), idempotencyKey);
    }

    public static TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromAccountId(),
                transfer.getToAccountId(),
                transfer.getAmount(),
                transfer.getCreatedAt());
    }

    public static StatementItemResponse toStatementItem(StatementEntry entry) {
        return new StatementItemResponse(
                entry.transferId(),
                entry.direction(),
                entry.counterpartAccountId(),
                entry.amount(),
                entry.createdAt());
    }
}
