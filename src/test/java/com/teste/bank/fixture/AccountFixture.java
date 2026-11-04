package com.teste.bank.fixture;

import com.teste.bank.api.dto.CreateAccountRequest;
import com.teste.bank.domain.model.Account;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class AccountFixture {

    public static final Long SOURCE_ID = 1L;
    public static final Long DESTINATION_ID = 2L;
    public static final Long NEW_ACCOUNT_ID = 4L;
    public static final Long UNKNOWN_ID = 999L;
    public static final String NAME = "Daniel";
    public static final int MAX_NAME_LENGTH = 120;

    private AccountFixture() {
    }

    public static Account account(Long id, BigDecimal balance) {
        return Account.builder()
                .id(id)
                .name(NAME)
                .balance(balance)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Account source(BigDecimal balance) {
        return account(SOURCE_ID, balance);
    }

    public static Account destination(BigDecimal balance) {
        return account(DESTINATION_ID, balance);
    }

    public static CreateAccountRequest createRequest(String name, BigDecimal initialBalance) {
        return CreateAccountRequest.builder()
                .name(name)
                .initialBalance(initialBalance)
                .build();
    }

    public static CreateAccountRequest validCreateRequest() {
        return createRequest(NAME, Amounts.BALANCE);
    }

    public static String oversizedName() {
        return "a".repeat(MAX_NAME_LENGTH + 1);
    }
}
