package com.teste.bank.application.service;

import org.springframework.data.domain.Page;

public interface TransferQueryService {

    Page<StatementEntry> findStatement(Long accountId, int page, int size);
}
