package com.teste.bank.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.teste.bank.application.service.TransferCommand;
import com.teste.bank.application.service.TransferResult;
import com.teste.bank.application.service.TransferService;
import com.teste.bank.domain.exception.InsufficientBalanceException;
import com.teste.bank.domain.exception.InvalidTransferException;
import com.teste.bank.domain.model.Transfer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    private static final String BODY = "{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":30.00}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    @Test
    void shouldReturnCreatedForNewTransfer() throws Exception {
        when(transferService.transfer(any(TransferCommand.class))).thenReturn(new TransferResult(transfer(), true));

        performTransfer("key-1", BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.amount").value(30.00));
    }

    @Test
    void shouldReturnOkForRepeatedIdempotencyKey() throws Exception {
        when(transferService.transfer(any(TransferCommand.class))).thenReturn(new TransferResult(transfer(), false));

        performTransfer("key-1", BODY).andExpect(status().isOk());
    }

    @Test
    void shouldRejectMissingIdempotencyKey() throws Exception {
        mockMvc.perform(post("/transfers").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectNonPositiveAmount() throws Exception {
        performTransfer("key-1", "{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    void shouldReturnUnprocessableWhenBalanceIsInsufficient() throws Exception {
        when(transferService.transfer(any(TransferCommand.class))).thenThrow(new InsufficientBalanceException(1L));

        performTransfer("key-1", BODY)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Saldo insuficiente"));
    }

    @Test
    void shouldReturnBadRequestWhenTransferIsInvalid() throws Exception {
        when(transferService.transfer(any(TransferCommand.class)))
                .thenThrow(new InvalidTransferException("Conta de origem e destino devem ser diferentes"));

        performTransfer("key-1", BODY).andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConflictOnConcurrentDuplicateKey() throws Exception {
        when(transferService.transfer(any(TransferCommand.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        performTransfer("key-1", BODY).andExpect(status().isConflict());
    }

    private ResultActions performTransfer(String key, String body) throws Exception {
        return mockMvc.perform(post("/transfers")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Transfer transfer() {
        Transfer transfer = new Transfer();
        transfer.setId(10L);
        transfer.setFromAccountId(1L);
        transfer.setToAccountId(2L);
        transfer.setAmount(new BigDecimal("30.00"));
        transfer.setCreatedAt(LocalDateTime.now());
        return transfer;
    }
}
