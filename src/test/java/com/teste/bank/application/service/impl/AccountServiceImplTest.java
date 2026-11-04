package com.teste.bank.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.exception.ErrorCode;
import com.teste.bank.domain.model.Account;
import com.teste.bank.domain.repository.AccountRepository;
import com.teste.bank.fixture.AccountFixture;
import com.teste.bank.fixture.Amounts;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    @Test
    void shouldCreateAccountWithTrimmedNameAndNormalizedBalance() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var account = accountService.create("  " + AccountFixture.NAME + "  ", Amounts.ONE_DECIMAL);

        assertThat(account.getName()).isEqualTo(AccountFixture.NAME);
        assertThat(account.getBalance()).isEqualTo(Amounts.ONE_DECIMAL_NORMALIZED);
        assertThat(account.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldCreateAccountWithZeroBalance() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var account = accountService.create(AccountFixture.NAME, Amounts.ZERO);

        assertThat(account.getBalance()).isEqualByComparingTo(Amounts.ZERO);
    }

    @Test
    void shouldReturnAccountWhenFound() {
        var account = AccountFixture.source(Amounts.BALANCE);
        when(accountRepository.findById(AccountFixture.SOURCE_ID)).thenReturn(Optional.of(account));

        assertThat(accountService.findById(AccountFixture.SOURCE_ID)).isSameAs(account);
    }

    @Test
    void shouldThrowWhenAccountNotFound() {
        when(accountRepository.findById(AccountFixture.UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findById(AccountFixture.UNKNOWN_ID))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage(ErrorCode.ACCOUNT_NOT_FOUND.message(AccountFixture.UNKNOWN_ID));
    }
}
