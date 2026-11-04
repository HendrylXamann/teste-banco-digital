package com.teste.bank.fixture;

import com.teste.bank.api.dto.TransferRequest;
import com.teste.bank.application.event.TransferCompletedEvent;
import com.teste.bank.application.service.TransferCommand;
import com.teste.bank.domain.model.Transfer;
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
        return new TransferCommand(from, to, amount, randomIdempotencyKey());
    }

    public static TransferCommand validCommand() {
        return command(AccountFixture.SOURCE_ID, AccountFixture.DESTINATION_ID, Amounts.TRANSFER);
    }

    public static TransferRequest request(Long from, Long to, BigDecimal amount) {
        return new TransferRequest(from, to, amount);
    }

    public static TransferRequest validRequest() {
        return request(AccountFixture.SOURCE_ID, AccountFixture.DESTINATION_ID, Amounts.TRANSFER);
    }

    public static Transfer transfer(Long id, Long from, Long to, BigDecimal amount) {
        Transfer transfer = new Transfer();
        transfer.setId(id);
        transfer.setIdempotencyKey(randomIdempotencyKey());
        transfer.setFromAccountId(from);
        transfer.setToAccountId(to);
        transfer.setAmount(amount);
        transfer.setCreatedAt(LocalDateTime.now());
        return transfer;
    }

    public static Transfer savedTransfer() {
        return transfer(TRANSFER_ID, AccountFixture.SOURCE_ID, AccountFixture.DESTINATION_ID, Amounts.TRANSFER);
    }

    public static TransferCompletedEvent completedEvent() {
        return new TransferCompletedEvent(
                TRANSFER_ID, AccountFixture.SOURCE_ID, AccountFixture.DESTINATION_ID, Amounts.TRANSFER);
    }
}
