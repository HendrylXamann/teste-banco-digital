package com.teste.bank.application.service;

import com.teste.bank.application.event.TransferCompletedEvent;
import com.teste.bank.application.validator.TransferValidator;
import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.model.Account;
import com.teste.bank.domain.model.Transfer;
import com.teste.bank.domain.repository.AccountRepository;
import com.teste.bank.domain.repository.TransferRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TransferResult transfer(TransferCommand command) {
        return transferRepository.findByIdempotencyKey(command.idempotencyKey())
                .map(existing -> new TransferResult(existing, false))
                .orElseGet(() -> new TransferResult(execute(command), true));
    }

    private Transfer execute(TransferCommand command) {
        BigDecimal amount = normalize(command.amount());
        TransferValidator.validateAmount(amount);
        TransferValidator.validateDistinctAccounts(command.fromAccountId(), command.toAccountId());
        LockedAccounts accounts = lockAccountsInOrder(command.fromAccountId(), command.toAccountId());
        TransferValidator.validateBalance(accounts.source(), amount);
        moveBalance(accounts, amount);
        Transfer transfer = saveTransfer(command, amount);
        publishEvent(transfer);
        return transfer;
    }

    private BigDecimal normalize(BigDecimal amount) {
        return amount == null ? null : amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    private LockedAccounts lockAccountsInOrder(Long fromAccountId, Long toAccountId) {
        Account first = lockAccount(Math.min(fromAccountId, toAccountId));
        Account second = lockAccount(Math.max(fromAccountId, toAccountId));
        return fromAccountId < toAccountId
                ? new LockedAccounts(first, second)
                : new LockedAccounts(second, first);
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
        Transfer transfer = new Transfer();
        transfer.setIdempotencyKey(command.idempotencyKey());
        transfer.setFromAccountId(command.fromAccountId());
        transfer.setToAccountId(command.toAccountId());
        transfer.setAmount(amount);
        transfer.setCreatedAt(LocalDateTime.now());
        return transferRepository.save(transfer);
    }

    private void publishEvent(Transfer transfer) {
        eventPublisher.publishEvent(new TransferCompletedEvent(
                transfer.getId(),
                transfer.getFromAccountId(),
                transfer.getToAccountId(),
                transfer.getAmount()));
    }

    private record LockedAccounts(Account source, Account destination) {
    }
}
