package com.teste.bank.application.service.impl;

import com.teste.bank.application.service.StatementEntry;
import com.teste.bank.application.service.TransferQueryService;
import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.model.Transfer;
import com.teste.bank.domain.model.TransferDirection;
import com.teste.bank.domain.repository.AccountRepository;
import com.teste.bank.domain.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferQueryServiceImpl implements TransferQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort NEWEST_FIRST = Sort.by(
            Sort.Order.desc("createdAt"), Sort.Order.desc("id")
    );

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<StatementEntry> findStatement(Long accountId, int page, int size) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), NEWEST_FIRST);
        return transferRepository.findStatementByAccountId(accountId, pageable)
                .map(transfer -> toEntry(transfer, accountId));
    }

    private StatementEntry toEntry(Transfer transfer, Long accountId) {
        boolean isDebit = transfer.getFromAccountId().equals(accountId);
        return new StatementEntry(
                transfer.getId(),
                isDebit ? TransferDirection.DEBIT : TransferDirection.CREDIT,
                isDebit ? transfer.getToAccountId() : transfer.getFromAccountId(),
                transfer.getAmount(),
                transfer.getCreatedAt());
    }
}
