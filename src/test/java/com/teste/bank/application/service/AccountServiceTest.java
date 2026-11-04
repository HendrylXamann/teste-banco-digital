package com.teste.bank.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.model.Account;
import com.teste.bank.domain.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldCreateAccountWithInitialBalance() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account account = accountService.create("  Daniel  ", new BigDecimal("250.5"));

        assertThat(account.getName()).isEqualTo("Daniel");
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("250.50"));
        assertThat(account.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldReturnAccountWhenFound() {
        Account account = new Account();
        account.setId(1L);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThat(accountService.findById(1L)).isSameAs(account);
    }

    @Test
    void shouldThrowWhenAccountNotFound() {
        when(accountRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findById(42L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining("42");
    }
}
