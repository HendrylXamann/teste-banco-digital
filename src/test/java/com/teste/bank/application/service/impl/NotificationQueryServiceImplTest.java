package com.teste.bank.application.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.model.NotificationStatus;
import com.teste.bank.domain.repository.AccountRepository;
import com.teste.bank.domain.repository.NotificationRepository;
import com.teste.bank.fixture.AccountFixture;
import com.teste.bank.fixture.NotificationFixture;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationQueryServiceImpl notificationQueryService;

    @Test
    void shouldReturnNotificationsNewestFirst() {
        var notification = NotificationFixture.notification(NotificationStatus.SENT);
        when(accountRepository.existsById(AccountFixture.SOURCE_ID)).thenReturn(true);
        when(notificationRepository.findByAccountId(eq(AccountFixture.SOURCE_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));
        var captor = ArgumentCaptor.forClass(Pageable.class);

        var page = notificationQueryService.findByAccount(AccountFixture.SOURCE_ID, -1, 1000);

        verify(notificationRepository).findByAccountId(eq(AccountFixture.SOURCE_ID), captor.capture());
        assertThat(page.getContent()).containsExactly(notification);
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void shouldThrowWhenAccountDoesNotExist() {
        when(accountRepository.existsById(AccountFixture.UNKNOWN_ID)).thenReturn(false);

        assertThatThrownBy(() -> notificationQueryService.findByAccount(AccountFixture.UNKNOWN_ID, 0, 20))
                .isInstanceOf(AccountNotFoundException.class);
        verifyNoInteractions(notificationRepository);
    }
}
