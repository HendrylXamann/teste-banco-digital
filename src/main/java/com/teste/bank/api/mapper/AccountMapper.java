package com.teste.bank.api.mapper;

import com.teste.bank.api.dto.AccountResponse;
import com.teste.bank.domain.model.Account;

public final class AccountMapper {

    private AccountMapper() {
    }

    public static AccountResponse toResponse(Account account) {
        return new AccountResponse(account.getId(), account.getName(), account.getBalance(), account.getCreatedAt());
    }
}
