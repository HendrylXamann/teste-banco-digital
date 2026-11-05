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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferQueryServiceImpl implements TransferQueryService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<StatementEntry> findStatement(Long accountId, int page, int size) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        return transferRepository.findStatementByAccountId(accountId, PageRequests.newestFirst(page, size))
                .map(transfer -> toEntry(transfer, accountId));
    }

    private StatementEntry toEntry(Transfer transfer, Long accountId) {
        var isDebit = transfer.getFromAccountId().equals(accountId);
        return StatementEntry.builder()
                .transferId(transfer.getId())
                .direction(isDebit ? TransferDirection.DEBIT : TransferDirection.CREDIT)
                .counterpartAccountId(isDebit ? transfer.getToAccountId() : transfer.getFromAccountId())
                .amount(transfer.getAmount())
                .createdAt(transfer.getCreatedAt())
                .build();
    }
}
