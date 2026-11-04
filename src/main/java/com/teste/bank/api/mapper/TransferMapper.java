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
        return TransferCommand.builder()
                .fromAccountId(request.fromAccountId())
                .toAccountId(request.toAccountId())
                .amount(request.amount())
                .idempotencyKey(idempotencyKey)
                .build();
    }

    public static TransferResponse toResponse(Transfer transfer) {
        return TransferResponse.builder()
                .id(transfer.getId())
                .fromAccountId(transfer.getFromAccountId())
                .toAccountId(transfer.getToAccountId())
                .amount(transfer.getAmount())
                .createdAt(transfer.getCreatedAt())
                .build();
    }

    public static StatementItemResponse toStatementItem(StatementEntry entry) {
        return StatementItemResponse.builder()
                .transferId(entry.transferId())
                .direction(entry.direction())
                .counterpartAccountId(entry.counterpartAccountId())
                .amount(entry.amount())
                .createdAt(entry.createdAt())
                .build();
    }
}
