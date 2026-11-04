package com.teste.bank.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.teste.bank.application.service.StatementEntry;
import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.model.Transfer;
import com.teste.bank.domain.model.TransferDirection;
import com.teste.bank.domain.repository.AccountRepository;
import com.teste.bank.domain.repository.TransferRepository;
import com.teste.bank.fixture.AccountFixture;
import com.teste.bank.fixture.Amounts;
import com.teste.bank.fixture.TransferFixture;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TransferQueryServiceImplTest {

    private static final Long THIRD_ACCOUNT_ID = 3L;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private TransferQueryServiceImpl transferQueryService;

    @Test
    void shouldMapDebitAndCreditDirections() {
        when(accountRepository.existsById(AccountFixture.SOURCE_ID)).thenReturn(true);
        List<Transfer> transfers = List.of(
                TransferFixture.transfer(TransferFixture.TRANSFER_ID, AccountFixture.SOURCE_ID,
                        AccountFixture.DESTINATION_ID, Amounts.TRANSFER),
                TransferFixture.transfer(TransferFixture.OTHER_TRANSFER_ID, THIRD_ACCOUNT_ID,
                        AccountFixture.SOURCE_ID, Amounts.SMALL));
        when(transferRepository.findStatementByAccountId(eq(AccountFixture.SOURCE_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(transfers));

        Page<StatementEntry> statement = transferQueryService.findStatement(AccountFixture.SOURCE_ID, 0, 20);

        assertThat(statement.getContent()).extracting(StatementEntry::direction)
                .containsExactly(TransferDirection.DEBIT, TransferDirection.CREDIT);
        assertThat(statement.getContent()).extracting(StatementEntry::counterpartAccountId)
                .containsExactly(AccountFixture.DESTINATION_ID, THIRD_ACCOUNT_ID);
    }

    @Test
    void shouldReturnEmptyStatementForAccountWithoutTransfers() {
        when(accountRepository.existsById(AccountFixture.SOURCE_ID)).thenReturn(true);
        when(transferRepository.findStatementByAccountId(eq(AccountFixture.SOURCE_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(transferQueryService.findStatement(AccountFixture.SOURCE_ID, 0, 20)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"-1, 1000, 0, 100", "0, 0, 0, 1", "2, 5, 2, 5"})
    void shouldNormalizePagination(int page, int size, int expectedPage, int expectedSize) {
        when(accountRepository.existsById(AccountFixture.SOURCE_ID)).thenReturn(true);
        when(transferRepository.findStatementByAccountId(eq(AccountFixture.SOURCE_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

        transferQueryService.findStatement(AccountFixture.SOURCE_ID, page, size);

        verify(transferRepository).findStatementByAccountId(eq(AccountFixture.SOURCE_ID), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(expectedPage);
        assertThat(captor.getValue().getPageSize()).isEqualTo(expectedSize);
    }

    @Test
    void shouldThrowWhenAccountDoesNotExist() {
        when(accountRepository.existsById(AccountFixture.UNKNOWN_ID)).thenReturn(false);

        assertThatThrownBy(() -> transferQueryService.findStatement(AccountFixture.UNKNOWN_ID, 0, 20))
                .isInstanceOf(AccountNotFoundException.class);
        verifyNoInteractions(transferRepository);
    }
}
