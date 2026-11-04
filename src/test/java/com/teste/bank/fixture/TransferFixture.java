package com.teste.bank.fixture;

import com.teste.bank.api.dto.TransferRequest;
import com.teste.bank.application.event.TransferCompletedEvent;
import com.teste.bank.application.service.StatementEntry;
import com.teste.bank.application.service.TransferCommand;
import com.teste.bank.application.service.TransferResult;
import com.teste.bank.domain.model.Transfer;
import com.teste.bank.domain.model.TransferDirection;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class TransferFixture {

    public static final Long TRANSFER_ID = 10L;
    public static final Long OTHER_TRANSFER_ID = 11L;
    public static final int MAX_IDEMPOTENCY_KEY_LENGTH = 64;
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    public static final String IDEMPOTENCY_CONSTRAINT = "uk_transfers_idempotency_key";

    private TransferFixture() {
    }

    public static String randomIdempotencyKey() {
        return UUID.randomUUID().toString();
    }

    public static String oversizedIdempotencyKey() {
        return "k".repeat(MAX_IDEMPOTENCY_KEY_LENGTH + 1);
    }

    public static TransferCommand command(Long from, Long to, BigDecimal amount) {
        return TransferCommand.builder()
                .fromAccountId(from)
                .toAccountId(to)
                .amount(amount)
                .idempotencyKey(randomIdempotencyKey())
                .build();
    }

    public static TransferCommand validCommand() {
        return command(AccountFixture.SOURCE_ID, AccountFixture.DESTINATION_ID, Amounts.TRANSFER);
    }

    public static TransferRequest request(Long from, Long to, BigDecimal amount) {
        return TransferRequest.builder()
                .fromAccountId(from)
                .toAccountId(to)
                .amount(amount)
                .build();
    }

    public static TransferRequest validRequest() {
        return request(AccountFixture.SOURCE_ID, AccountFixture.DESTINATION_ID, Amounts.TRANSFER);
    }

    public static Transfer transfer(Long id, Long from, Long to, BigDecimal amount) {
        return Transfer.builder()
                .id(id)
                .idempotencyKey(randomIdempotencyKey())
                .fromAccountId(from)
                .toAccountId(to)
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Transfer savedTransfer() {
        return transfer(TRANSFER_ID, AccountFixture.SOURCE_ID, AccountFixture.DESTINATION_ID, Amounts.TRANSFER);
    }

    public static TransferResult result(Transfer transfer, boolean created) {
        return TransferResult.builder()
                .transfer(transfer)
                .created(created)
                .build();
    }

    public static StatementEntry statementEntry(TransferDirection direction, Long counterpartAccountId) {
        return StatementEntry.builder()
                .transferId(TRANSFER_ID)
                .direction(direction)
                .counterpartAccountId(counterpartAccountId)
                .amount(Amounts.TRANSFER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static TransferCompletedEvent completedEvent() {
        return TransferCompletedEvent.builder()
                .transferId(TRANSFER_ID)
                .fromAccountId(AccountFixture.SOURCE_ID)
                .toAccountId(AccountFixture.DESTINATION_ID)
                .amount(Amounts.TRANSFER)
                .build();
    }
}
