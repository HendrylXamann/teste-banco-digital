package com.teste.bank.api.mapper;

import com.teste.bank.api.dto.AccountResponse;
import com.teste.bank.domain.model.Account;

public final class AccountMapper {

    private AccountMapper() {
    }

    public static AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .name(account.getName())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }
}
