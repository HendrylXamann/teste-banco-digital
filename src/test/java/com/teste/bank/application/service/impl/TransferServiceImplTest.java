package com.teste.bank.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.teste.bank.application.event.TransferCompletedEvent;
import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.exception.ErrorCode;
import com.teste.bank.domain.exception.InsufficientBalanceException;
import com.teste.bank.domain.exception.InvalidTransferException;
import com.teste.bank.domain.model.Account;
import com.teste.bank.domain.model.Transfer;
import com.teste.bank.domain.repository.AccountRepository;
import com.teste.bank.domain.repository.TransferRepository;
import com.teste.bank.fixture.AccountFixture;
import com.teste.bank.fixture.Amounts;
import com.teste.bank.fixture.TransferFixture;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransferServiceImpl transferService;

    static Stream<Arguments> invalidAmounts() {
        return Stream.of(Arguments.of((BigDecimal) null), Arguments.of(Amounts.ZERO), Arguments.of(Amounts.NEGATIVE));
    }

    @Test
    void shouldTransferAndUpdateBothBalances() {
        var source = stubLocked(AccountFixture.source(Amounts.BALANCE));
        var destination = stubLocked(AccountFixture.destination(Amounts.SMALL));
        stubSaveTransfer();

        var result = transferService.transfer(TransferFixture.validCommand());

        assertThat(result.created()).isTrue();
        assertThat(source.getBalance()).isEqualByComparingTo(Amounts.BALANCE.subtract(Amounts.TRANSFER));
        assertThat(destination.getBalance()).isEqualByComparingTo(Amounts.SMALL.add(Amounts.TRANSFER));
        assertThat(result.transfer().getAmount()).isEqualByComparingTo(Amounts.TRANSFER);
    }

    @Test
    void shouldAllowTransferOfEntireBalance() {
        var source = stubLocked(AccountFixture.source(Amounts.TRANSFER));
        stubLocked(AccountFixture.destination(Amounts.ZERO));
        stubSaveTransfer();

        transferService.transfer(TransferFixture.validCommand());

        assertThat(source.getBalance()).isEqualByComparingTo(Amounts.ZERO);
    }

    @Test
    void shouldNormalizeAmountToTwoDecimals() {
        stubLocked(AccountFixture.source(Amounts.BALANCE));
        stubLocked(AccountFixture.destination(Amounts.ZERO));
        stubSaveTransfer();
        var command = TransferFixture.command(
                AccountFixture.SOURCE_ID, AccountFixture.DESTINATION_ID, Amounts.ONE_DECIMAL);

        var result = transferService.transfer(command);

        assertThat(result.transfer().getAmount()).isEqualTo(Amounts.ONE_DECIMAL_NORMALIZED);
    }

    @Test
    void shouldReturnExistingTransferWhenIdempotencyKeyAlreadyUsed() {
        var existing = TransferFixture.savedTransfer();
        var command = TransferFixture.validCommand();
        when(transferRepository.findByIdempotencyKey(command.idempotencyKey())).thenReturn(Optional.of(existing));

        var result = transferService.transfer(command);

        assertThat(result.created()).isFalse();
        assertThat(result.transfer()).isSameAs(existing);
        verifyNoInteractions(accountRepository, eventPublisher);
    }

    @Test
    void shouldThrowWhenSourceAccountNotFound() {
        when(accountRepository.findByIdForUpdate(AccountFixture.SOURCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(TransferFixture.validCommand()))
                .isInstanceOf(AccountNotFoundException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    void shouldThrowWhenDestinationAccountNotFound() {
        stubLocked(AccountFixture.source(Amounts.BALANCE));
        when(accountRepository.findByIdForUpdate(AccountFixture.DESTINATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(TransferFixture.validCommand()))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void shouldRejectSameAccountWithoutLocking() {
        var command = TransferFixture.command(
                AccountFixture.SOURCE_ID, AccountFixture.SOURCE_ID, Amounts.TRANSFER);

        assertThatThrownBy(() -> transferService.transfer(command))
                .isInstanceOf(InvalidTransferException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SAME_ACCOUNT_TRANSFER);
        verifyNoInteractions(accountRepository);
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void shouldRejectInvalidAmountWithoutLocking(BigDecimal amount) {
        var command = TransferFixture.command(
                AccountFixture.SOURCE_ID, AccountFixture.DESTINATION_ID, amount);

        assertThatThrownBy(() -> transferService.transfer(command))
                .isInstanceOf(InvalidTransferException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AMOUNT);
        verifyNoInteractions(accountRepository);
    }

    @Test
    void shouldLockAccountsInAscendingIdOrder() {
        stubLocked(AccountFixture.source(Amounts.ZERO));
        stubLocked(AccountFixture.destination(Amounts.BALANCE));
        stubSaveTransfer();
        var reverse = TransferFixture.command(
                AccountFixture.DESTINATION_ID, AccountFixture.SOURCE_ID, Amounts.TRANSFER);

        transferService.transfer(reverse);

        var lockOrder = inOrder(accountRepository);
        lockOrder.verify(accountRepository).findByIdForUpdate(AccountFixture.SOURCE_ID);
        lockOrder.verify(accountRepository).findByIdForUpdate(AccountFixture.DESTINATION_ID);
    }

    @Test
    void shouldPublishTransferCompletedEvent() {
        stubLocked(AccountFixture.source(Amounts.BALANCE));
        stubLocked(AccountFixture.destination(Amounts.ZERO));
        stubSaveTransfer();
        var captor = ArgumentCaptor.forClass(TransferCompletedEvent.class);

        transferService.transfer(TransferFixture.validCommand());

        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().transferId()).isEqualTo(TransferFixture.TRANSFER_ID);
        assertThat(captor.getValue().amount()).isEqualByComparingTo(Amounts.TRANSFER);
    }

    @Test
    void shouldNotSaveNorPublishWhenBalanceIsInsufficient() {
        var source = stubLocked(AccountFixture.source(Amounts.SMALL));
        stubLocked(AccountFixture.destination(Amounts.ZERO));

        assertThatThrownBy(() -> transferService.transfer(TransferFixture.validCommand()))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(source.getBalance()).isEqualByComparingTo(Amounts.SMALL);
        verify(transferRepository, never()).save(any(Transfer.class));
        verifyNoInteractions(eventPublisher);
    }

    private Account stubLocked(Account account) {
        when(accountRepository.findByIdForUpdate(account.getId())).thenReturn(Optional.of(account));
        return account;
    }

    private void stubSaveTransfer() {
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            var transfer = invocation.getArgument(0, Transfer.class);
            transfer.setId(TransferFixture.TRANSFER_ID);
            return transfer;
        });
    }
}
