package com.teste.bank.application.service;

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
import com.teste.bank.domain.exception.InsufficientBalanceException;
import com.teste.bank.domain.exception.InvalidTransferException;
import com.teste.bank.domain.model.Account;
import com.teste.bank.domain.model.Transfer;
import com.teste.bank.domain.repository.AccountRepository;
import com.teste.bank.domain.repository.TransferRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final String KEY = "key-1";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransferService transferService;

    @Test
    void shouldTransferAndUpdateBothBalances() {
        Account source = stubLockedAccount(1L, "100.00");
        Account destination = stubLockedAccount(2L, "50.00");
        stubSaveTransfer();

        TransferResult result = transferService.transfer(command(1L, 2L, "30.00"));

        assertThat(result.created()).isTrue();
        assertThat(source.getBalance()).isEqualByComparingTo("70.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("80.00");
        assertThat(result.transfer().getAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void shouldReturnExistingTransferWhenIdempotencyKeyAlreadyUsed() {
        Transfer existing = new Transfer();
        existing.setId(10L);
        when(transferRepository.findByIdempotencyKey(KEY)).thenReturn(Optional.of(existing));

        TransferResult result = transferService.transfer(command(1L, 2L, "30.00"));

        assertThat(result.created()).isFalse();
        assertThat(result.transfer()).isSameAs(existing);
        verifyNoInteractions(accountRepository, eventPublisher);
    }

    @Test
    void shouldThrowWhenSourceAccountNotFound() {
        when(accountRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(command(1L, 2L, "30.00")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void shouldThrowWhenDestinationAccountNotFound() {
        stubLockedAccount(1L, "100.00");
        when(accountRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.transfer(command(1L, 2L, "30.00")))
                .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    void shouldThrowWhenAccountsAreTheSame() {
        assertThatThrownBy(() -> transferService.transfer(command(1L, 1L, "30.00")))
                .isInstanceOf(InvalidTransferException.class);

        verifyNoInteractions(accountRepository);
    }

    @Test
    void shouldLockAccountsInAscendingIdOrder() {
        Account source = stubLockedAccount(5L, "100.00");
        Account destination = stubLockedAccount(2L, "0.00");
        stubSaveTransfer();

        transferService.transfer(command(5L, 2L, "40.00"));

        InOrder lockOrder = inOrder(accountRepository);
        lockOrder.verify(accountRepository).findByIdForUpdate(2L);
        lockOrder.verify(accountRepository).findByIdForUpdate(5L);
        assertThat(source.getBalance()).isEqualByComparingTo("60.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("40.00");
    }

    @Test
    void shouldPublishTransferCompletedEvent() {
        stubLockedAccount(1L, "100.00");
        stubLockedAccount(2L, "0.00");
        stubSaveTransfer();
        ArgumentCaptor<TransferCompletedEvent> captor = ArgumentCaptor.forClass(TransferCompletedEvent.class);

        transferService.transfer(command(1L, 2L, "25.00"));

        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().transferId()).isEqualTo(99L);
        assertThat(captor.getValue().amount()).isEqualByComparingTo("25.00");
    }

    @Test
    void shouldNotPublishEventWhenTransferFails() {
        stubLockedAccount(1L, "10.00");
        stubLockedAccount(2L, "0.00");

        assertThatThrownBy(() -> transferService.transfer(command(1L, 2L, "25.00")))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(transferRepository, never()).save(any(Transfer.class));
        verifyNoInteractions(eventPublisher);
    }

    private TransferCommand command(Long from, Long to, String amount) {
        return new TransferCommand(from, to, new BigDecimal(amount), KEY);
    }

    private Account stubLockedAccount(Long id, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setBalance(new BigDecimal(balance));
        when(accountRepository.findByIdForUpdate(id)).thenReturn(Optional.of(account));
        return account;
    }

    private void stubSaveTransfer() {
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            transfer.setId(99L);
            return transfer;
        });
    }
}
