package com.teste.bank.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.model.Transfer;
import com.teste.bank.domain.model.TransferDirection;
import com.teste.bank.domain.repository.AccountRepository;
import com.teste.bank.domain.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TransferQueryServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private TransferQueryService transferQueryService;

    @Test
    void shouldMapDebitAndCreditDirections() {
        when(accountRepository.existsById(1L)).thenReturn(true);
        List<Transfer> transfers = List.of(transfer(10L, 1L, 2L), transfer(11L, 3L, 1L));
        when(transferRepository.findStatementByAccountId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(transfers));

        Page<StatementEntry> statement = transferQueryService.findStatement(1L, 0, 20);

        assertThat(statement.getContent()).extracting(StatementEntry::direction)
                .containsExactly(TransferDirection.DEBIT, TransferDirection.CREDIT);
        assertThat(statement.getContent()).extracting(StatementEntry::counterpartAccountId)
                .containsExactly(2L, 3L);
    }

    @Test
    void shouldLimitPageSize() {
        when(accountRepository.existsById(1L)).thenReturn(true);
        when(transferRepository.findStatementByAccountId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        transferQueryService.findStatement(1L, -1, 1000);

        verify(transferRepository).findStatementByAccountId(eq(1L), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void shouldThrowWhenAccountDoesNotExist() {
        when(accountRepository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> transferQueryService.findStatement(9L, 0, 20))
                .isInstanceOf(AccountNotFoundException.class);

        verifyNoInteractions(transferRepository);
    }

    private Transfer transfer(Long id, Long from, Long to) {
        Transfer transfer = new Transfer();
        transfer.setId(id);
        transfer.setFromAccountId(from);
        transfer.setToAccountId(to);
        transfer.setAmount(new BigDecimal("10.00"));
        transfer.setCreatedAt(LocalDateTime.now());
        return transfer;
    }
}
