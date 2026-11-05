package com.teste.bank.api.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teste.bank.application.service.NotificationQueryService;
import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.exception.ErrorCode;
import com.teste.bank.domain.model.NotificationStatus;
import com.teste.bank.domain.model.TransferDirection;
import com.teste.bank.fixture.AccountFixture;
import com.teste.bank.fixture.NotificationFixture;
import com.teste.bank.fixture.TransferFixture;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    private static final String NOTIFICATIONS = "/accounts/{id}/notifications";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationQueryService notificationQueryService;

    @Test
    void shouldReturnNotificationsOfAccount() throws Exception {
        var notification = NotificationFixture.notification(NotificationStatus.SENT);
        when(notificationQueryService.findByAccount(AccountFixture.SOURCE_ID, 0, 20))
                .thenReturn(new PageImpl<>(List.of(notification)));

        mockMvc.perform(get(NOTIFICATIONS, AccountFixture.SOURCE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transferId").value(TransferFixture.TRANSFER_ID))
                .andExpect(jsonPath("$.content[0].direction").value(TransferDirection.DEBIT.name()))
                .andExpect(jsonPath("$.content[0].status").value(NotificationStatus.SENT.name()))
                .andExpect(jsonPath("$.content[0].message").value(NotificationFixture.MESSAGE))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldForwardPaginationParameters() throws Exception {
        when(notificationQueryService.findByAccount(AccountFixture.SOURCE_ID, 2, 5))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(NOTIFICATIONS, AccountFixture.SOURCE_ID).param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        verify(notificationQueryService).findByAccount(AccountFixture.SOURCE_ID, 2, 5);
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
        when(notificationQueryService.findByAccount(AccountFixture.UNKNOWN_ID, 0, 20))
                .thenThrow(new AccountNotFoundException(AccountFixture.UNKNOWN_ID));

        mockMvc.perform(get(NOTIFICATIONS, AccountFixture.UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCOUNT_NOT_FOUND.name()));
    }

    @Test
    void shouldRejectNonNumericAccountId() throws Exception {
        mockMvc.perform(get(NOTIFICATIONS, "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETER.name()));
    }
}
