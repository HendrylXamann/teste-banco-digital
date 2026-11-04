package com.teste.bank.api.controller;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teste.bank.api.dto.CreateAccountRequest;
import com.teste.bank.application.service.AccountService;
import com.teste.bank.application.service.StatementEntry;
import com.teste.bank.application.service.TransferQueryService;
import com.teste.bank.domain.exception.AccountNotFoundException;
import com.teste.bank.domain.exception.ErrorCode;
import com.teste.bank.domain.model.TransferDirection;
import com.teste.bank.fixture.AccountFixture;
import com.teste.bank.fixture.Amounts;
import com.teste.bank.fixture.TransferFixture;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.CannotCreateTransactionException;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    private static final String ACCOUNTS = "/accounts";
    private static final String ACCOUNT_BY_ID = "/accounts/{id}";
    private static final String STATEMENT = "/accounts/{id}/transfers";
    private static final String MALFORMED_JSON = "{\"name\": ";
    private static final String INTERNAL_DETAIL = "detalhe interno que nao pode vazar";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private TransferQueryService transferQueryService;

    static Stream<Arguments> invalidCreateRequests() {
        return Stream.of(
                Arguments.of(AccountFixture.createRequest("", Amounts.BALANCE), "name"),
                Arguments.of(AccountFixture.createRequest("   ", Amounts.BALANCE), "name"),
                Arguments.of(AccountFixture.createRequest(null, Amounts.BALANCE), "name"),
                Arguments.of(AccountFixture.createRequest(AccountFixture.oversizedName(), Amounts.BALANCE), "name"),
                Arguments.of(AccountFixture.createRequest(AccountFixture.NAME, null), "initialBalance"),
                Arguments.of(AccountFixture.createRequest(AccountFixture.NAME, Amounts.NEGATIVE), "initialBalance"),
                Arguments.of(AccountFixture.createRequest(AccountFixture.NAME, Amounts.THREE_DECIMALS),
                        "initialBalance"));
    }

    @Test
    void shouldCreateAccountAndReturnLocation() throws Exception {
        when(accountService.create(eq(AccountFixture.NAME), any(BigDecimal.class)))
                .thenReturn(AccountFixture.account(AccountFixture.NEW_ACCOUNT_ID, Amounts.BALANCE));

        postAccount(objectMapper.writeValueAsString(AccountFixture.validCreateRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", ACCOUNTS + "/" + AccountFixture.NEW_ACCOUNT_ID))
                .andExpect(jsonPath("$.id").value(AccountFixture.NEW_ACCOUNT_ID))
                .andExpect(jsonPath("$.balance").value(Amounts.BALANCE.doubleValue()));
    }

    @ParameterizedTest
    @MethodSource("invalidCreateRequests")
    void shouldRejectInvalidCreateRequest(CreateAccountRequest request, String field) throws Exception {
        postAccount(objectMapper.writeValueAsString(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_FIELDS.name()))
                .andExpect(jsonPath("$.errors." + field).exists());
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        postAccount(MALFORMED_JSON)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()));
    }

    @Test
    void shouldRejectMissingBody() throws Exception {
        mockMvc.perform(post(ACCOUNTS).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()));
    }

    @Test
    void shouldRejectUnsupportedContentType() throws Exception {
        mockMvc.perform(post(ACCOUNTS).contentType(MediaType.TEXT_PLAIN).content(AccountFixture.NAME))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void shouldReturnAccount() throws Exception {
        when(accountService.findById(AccountFixture.SOURCE_ID))
                .thenReturn(AccountFixture.source(Amounts.BALANCE));

        mockMvc.perform(get(ACCOUNT_BY_ID, AccountFixture.SOURCE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(AccountFixture.NAME));
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
        when(accountService.findById(AccountFixture.UNKNOWN_ID))
                .thenThrow(new AccountNotFoundException(AccountFixture.UNKNOWN_ID));

        mockMvc.perform(get(ACCOUNT_BY_ID, AccountFixture.UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCOUNT_NOT_FOUND.name()))
                .andExpect(jsonPath("$.title").value(ErrorCode.ACCOUNT_NOT_FOUND.getTitle()));
    }

    @Test
    void shouldRejectNonNumericAccountId() throws Exception {
        mockMvc.perform(get(ACCOUNT_BY_ID, "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETER.name()));
    }

    @Test
    void shouldReturnStatement() throws Exception {
        StatementEntry entry = new StatementEntry(TransferFixture.TRANSFER_ID, TransferDirection.DEBIT,
                AccountFixture.DESTINATION_ID, Amounts.TRANSFER, LocalDateTime.now());
        when(transferQueryService.findStatement(AccountFixture.SOURCE_ID, 0, 20))
                .thenReturn(new PageImpl<>(List.of(entry)));

        mockMvc.perform(get(STATEMENT, AccountFixture.SOURCE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].direction").value(TransferDirection.DEBIT.name()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldForwardPaginationParameters() throws Exception {
        when(transferQueryService.findStatement(AccountFixture.SOURCE_ID, 2, 5)).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(STATEMENT, AccountFixture.SOURCE_ID).param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        verify(transferQueryService).findStatement(AccountFixture.SOURCE_ID, 2, 5);
    }

    @Test
    void shouldRejectNonNumericPageParameter() throws Exception {
        mockMvc.perform(get(STATEMENT, AccountFixture.SOURCE_ID).param("page", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_PARAMETER.name()));
    }

    @Test
    void shouldReturnServiceUnavailableWhenDatabaseIsDown() throws Exception {
        when(accountService.findById(AccountFixture.SOURCE_ID))
                .thenThrow(new CannotCreateTransactionException(INTERNAL_DETAIL));

        mockMvc.perform(get(ACCOUNT_BY_ID, AccountFixture.SOURCE_ID))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(ErrorCode.DATABASE_UNAVAILABLE.name()))
                .andExpect(jsonPath("$.detail").value(not(containsString(INTERNAL_DETAIL))));
    }

    @Test
    void shouldReturnGenericErrorWithoutLeakingDetails() throws Exception {
        when(accountService.findById(AccountFixture.SOURCE_ID)).thenThrow(new IllegalStateException(INTERNAL_DETAIL));

        mockMvc.perform(get(ACCOUNT_BY_ID, AccountFixture.SOURCE_ID))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.name()))
                .andExpect(jsonPath("$.detail").value(ErrorCode.INTERNAL_ERROR.message()));
    }

    private ResultActions postAccount(String body) throws Exception {
        return mockMvc.perform(post(ACCOUNTS).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
