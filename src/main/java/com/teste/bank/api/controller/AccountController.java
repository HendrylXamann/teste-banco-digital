package com.teste.bank.api.controller;

import com.teste.bank.api.dto.AccountResponse;
import com.teste.bank.api.dto.CreateAccountRequest;
import com.teste.bank.api.dto.PageResponse;
import com.teste.bank.api.dto.StatementItemResponse;
import com.teste.bank.api.mapper.AccountMapper;
import com.teste.bank.api.mapper.TransferMapper;
import com.teste.bank.application.service.AccountService;
import com.teste.bank.application.service.TransferQueryService;
import com.teste.bank.domain.model.Account;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Contas", description = "Cadastro, consulta de saldo e extrato")
public class AccountController {

    private final AccountService accountService;
    private final TransferQueryService transferQueryService;

    @PostMapping
    @Operation(summary = "Cria uma conta com saldo inicial")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.create(request.name(), request.initialBalance());
        URI location = URI.create("/accounts/" + account.getId());
        return ResponseEntity.created(location).body(AccountMapper.toResponse(account));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma conta e seu saldo")
    public AccountResponse findById(@PathVariable Long id) {
        return AccountMapper.toResponse(accountService.findById(id));
    }

    @GetMapping("/{id}/transfers")
    @Operation(summary = "Extrato paginado da conta, mais recentes primeiro")
    public PageResponse<StatementItemResponse> statement(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(transferQueryService.findStatement(id, page, size), TransferMapper::toStatementItem);
    }
}
