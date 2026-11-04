package com.teste.bank.application.service.impl;

import com.teste.bank.application.event.TransferCompletedEvent;
import com.teste.bank.application.service.TransferCommand;
import com.teste.bank.application.service.TransferResult;
import com.teste.bank.application.service.TransferService;
import com.teste.bank.application.validator.TransferValidator;
import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.model.Account;
import com.teste.bank.domain.model.Transfer;
import com.teste.bank.domain.repository.AccountRepository;
import com.teste.bank.domain.repository.TransferRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public TransferResult transfer(TransferCommand command) {
        return transferRepository.findByIdempotencyKey(command.idempotencyKey())
                .map(existing -> TransferResult.builder().transfer(existing).created(false).build())
                .orElseGet(() -> TransferResult.builder().transfer(execute(command)).created(true).build());
    }

    private Transfer execute(TransferCommand command) {
        var amount = normalize(command.amount());
        TransferValidator.validateAmount(amount);
        TransferValidator.validateDistinctAccounts(command.fromAccountId(), command.toAccountId());
        var accounts = lockAccountsInOrder(command.fromAccountId(), command.toAccountId());
        TransferValidator.validateBalance(accounts.source(), amount);
        moveBalance(accounts, amount);
        var transfer = saveTransfer(command, amount);
        publishEvent(transfer);
        return transfer;
    }

    private BigDecimal normalize(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    private LockedAccounts lockAccountsInOrder(Long fromAccountId, Long toAccountId) {
        var first = lockAccount(Math.min(fromAccountId, toAccountId));
        var second = lockAccount(Math.max(fromAccountId, toAccountId));
        var sourceIsFirst = fromAccountId < toAccountId;
        return LockedAccounts.builder()
                .source(sourceIsFirst ? first : second)
                .destination(sourceIsFirst ? second : first)
                .build();
    }

    private Account lockAccount(Long accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    private void moveBalance(LockedAccounts accounts, BigDecimal amount) {
        accounts.source().setBalance(accounts.source().getBalance().subtract(amount));
        accounts.destination().setBalance(accounts.destination().getBalance().add(amount));
    }

    private Transfer saveTransfer(TransferCommand command, BigDecimal amount) {
        var transfer = Transfer.builder()
                .idempotencyKey(command.idempotencyKey())
                .fromAccountId(command.fromAccountId())
                .toAccountId(command.toAccountId())
                .amount(amount)
                .createdAt(LocalDateTime.now())
                .build();
        return transferRepository.save(transfer);
    }

    private void publishEvent(Transfer transfer) {
        eventPublisher.publishEvent(TransferCompletedEvent.builder()
                .transferId(transfer.getId())
                .fromAccountId(transfer.getFromAccountId())
                .toAccountId(transfer.getToAccountId())
                .amount(transfer.getAmount())
                .build());
    }

    @Builder
    private record LockedAccounts(Account source, Account destination) {
    }
}
