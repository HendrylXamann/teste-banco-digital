package com.teste.bank.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teste.bank.application.service.AccountService;
import com.teste.bank.application.service.StatementEntry;
import com.teste.bank.application.service.TransferQueryService;
import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.model.Account;
import com.teste.bank.domain.model.TransferDirection;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private TransferQueryService transferQueryService;

    @Test
    void shouldCreateAccountAndReturnLocation() throws Exception {
        when(accountService.create(eq("Daniel"), any(BigDecimal.class))).thenReturn(account(4L, "250.00"));

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Daniel\",\"initialBalance\":250.00}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/accounts/4"))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.balance").value(250.00));
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"initialBalance\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.initialBalance").exists());
    }

    @Test
    void shouldReturnAccount() throws Exception {
        when(accountService.findById(1L)).thenReturn(account(1L, "1000.00"));

        mockMvc.perform(get("/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Conta 1"));
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
        when(accountService.findById(99L)).thenThrow(new AccountNotFoundException(99L));

        mockMvc.perform(get("/accounts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Conta não encontrada"));
    }

    @Test
    void shouldReturnStatement() throws Exception {
        StatementEntry entry = new StatementEntry(10L, TransferDirection.DEBIT, 2L, new BigDecimal("30.00"), LocalDateTime.now());
        when(transferQueryService.findStatement(1L, 0, 20)).thenReturn(new PageImpl<>(List.of(entry)));

        mockMvc.perform(get("/accounts/1/transfers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].direction").value("DEBIT"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private Account account(Long id, String balance) {
        Account account = new Account();
        account.setId(id);
        account.setName(id == 4L ? "Daniel" : "Conta " + id);
        account.setBalance(new BigDecimal(balance));
        account.setCreatedAt(LocalDateTime.now());
        return account;
    }
}
