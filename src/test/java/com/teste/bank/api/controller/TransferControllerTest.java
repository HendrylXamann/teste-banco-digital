package com.teste.bank.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teste.bank.api.dto.TransferRequest;
import com.teste.bank.application.service.TransferCommand;
import com.teste.bank.application.service.TransferResult;
import com.teste.bank.application.service.TransferService;
import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.exception.ErrorCode;
import com.teste.bank.domain.exception.InsufficientBalanceException;
import com.teste.bank.domain.exception.InvalidTransferException;
import com.teste.bank.fixture.AccountFixture;
import com.teste.bank.fixture.Amounts;
import com.teste.bank.fixture.TransferFixture;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(TransferController.class)
class TransferControllerTest {

    private static final String TRANSFERS = "/transfers";
    private static final String MALFORMED_JSON = "{\"fromAccountId\": 1, ";
    private static final String OTHER_CONSTRAINT = "ck_transfers_distinct_accounts";
    private static final String INTERNAL_DETAIL = "detalhe interno que nao pode vazar";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransferService transferService;

    static Stream<Arguments> invalidRequests() {
        Long source = AccountFixture.SOURCE_ID;
        Long destination = AccountFixture.DESTINATION_ID;
        return Stream.of(
                Arguments.of(TransferFixture.request(null, destination, Amounts.TRANSFER), "fromAccountId"),
                Arguments.of(TransferFixture.request(source, null, Amounts.TRANSFER), "toAccountId"),
                Arguments.of(TransferFixture.request(source, destination, null), "amount"),
                Arguments.of(TransferFixture.request(source, destination, Amounts.ZERO), "amount"),
                Arguments.of(TransferFixture.request(source, destination, Amounts.NEGATIVE), "amount"),
                Arguments.of(TransferFixture.request(source, destination, Amounts.THREE_DECIMALS), "amount"));
    }

    static Stream<Arguments> invalidIdempotencyKeys() {
        return Stream.of(Arguments.of(" "), Arguments.of(TransferFixture.oversizedIdempotencyKey()));
    }

    @Test
    void shouldReturnCreatedForNewTransfer() throws Exception {
        stubTransferResult(true);

        performTransfer(TransferFixture.randomIdempotencyKey(), TransferFixture.validRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TransferFixture.TRANSFER_ID))
                .andExpect(jsonPath("$.amount").value(Amounts.TRANSFER.doubleValue()));
    }

    @Test
    void shouldReturnOkForRepeatedIdempotencyKey() throws Exception {
        stubTransferResult(false);

        performTransfer(TransferFixture.randomIdempotencyKey(), TransferFixture.validRequest())
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void shouldRejectInvalidPayload(TransferRequest request, String field) throws Exception {
        performTransfer(TransferFixture.randomIdempotencyKey(), request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELDS.name()))
                .andExpect(jsonPath("$.errors." + field).exists());
        verifyNoInteractions(transferService);
    }

    @Test
    void shouldRejectMissingIdempotencyKey() throws Exception {
        mockMvc.perform(post(TRANSFERS).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(TransferFixture.validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MISSING_HEADER.name()));
        verifyNoInteractions(transferService);
    }

    @ParameterizedTest
    @MethodSource("invalidIdempotencyKeys")
    void shouldRejectInvalidIdempotencyKey(String key) throws Exception {
        performTransfer(key, TransferFixture.validRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.idempotencyKey").exists());
        verifyNoInteractions(transferService);
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post(TRANSFERS)
                        .header(TransferFixture.IDEMPOTENCY_HEADER, TransferFixture.randomIdempotencyKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MALFORMED_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()));
    }

    @Test
    void shouldReturnUnprocessableWhenBalanceIsInsufficient() throws Exception {
        stubTransferFailure(new InsufficientBalanceException(AccountFixture.SOURCE_ID));

        expectError(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.INSUFFICIENT_BALANCE);
    }

    @Test
    void shouldReturnBadRequestForSameAccount() throws Exception {
        stubTransferFailure(new InvalidTransferException(ErrorCode.SAME_ACCOUNT_TRANSFER));

        expectError(HttpStatus.BAD_REQUEST, ErrorCode.SAME_ACCOUNT_TRANSFER);
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
        stubTransferFailure(new AccountNotFoundException(AccountFixture.UNKNOWN_ID));

        expectError(HttpStatus.NOT_FOUND, ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    void shouldReturnConflictWhenIdempotencyKeyRacesInDatabase() throws Exception {
        stubTransferFailure(new DataIntegrityViolationException(TransferFixture.IDEMPOTENCY_CONSTRAINT));

        expectError(HttpStatus.CONFLICT, ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
    }

    @Test
    void shouldReturnUnprocessableWhenOtherDatabaseConstraintFails() throws Exception {
        stubTransferFailure(new DataIntegrityViolationException(OTHER_CONSTRAINT));

        expectError(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.DATA_INTEGRITY_VIOLATION);
    }

    @Test
    void shouldReturnConflictWhenAccountLockTimesOut() throws Exception {
        stubTransferFailure(new CannotAcquireLockException(INTERNAL_DETAIL));

        expectError(HttpStatus.CONFLICT, ErrorCode.ACCOUNT_LOCKED);
    }

    @Test
    void shouldReturnGenericErrorForUnexpectedFailure() throws Exception {
        stubTransferFailure(new NullPointerException(INTERNAL_DETAIL));

        expectError(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR);
    }

    private void stubTransferResult(boolean created) {
        when(transferService.transfer(any(TransferCommand.class)))
                .thenReturn(new TransferResult(TransferFixture.savedTransfer(), created));
    }

    private void stubTransferFailure(RuntimeException exception) {
        when(transferService.transfer(any(TransferCommand.class))).thenThrow(exception);
    }

    private void expectError(HttpStatus expectedStatus, ErrorCode errorCode) throws Exception {
        performTransfer(TransferFixture.randomIdempotencyKey(), TransferFixture.validRequest())
                .andExpect(status().is(expectedStatus.value()))
                .andExpect(jsonPath("$.code").value(errorCode.name()))
                .andExpect(jsonPath("$.title").value(errorCode.getTitle()));
    }

    private ResultActions performTransfer(String key, TransferRequest request) throws Exception {
        return mockMvc.perform(post(TRANSFERS)
                .header(TransferFixture.IDEMPOTENCY_HEADER, key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}
