package com.teste.bank.api.controller;

import com.teste.bank.api.dto.TransferRequest;
import com.teste.bank.api.dto.TransferResponse;
import com.teste.bank.api.mapper.TransferMapper;
import com.teste.bank.application.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
@Tag(name = "Transferências", description = "Transferência de valores entre contas")
public class TransferController {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final TransferService transferService;

    @PostMapping
    @Operation(summary = "Transfere valor entre duas contas",
            description = "Retorna 201 para transferência nova e 200 quando a Idempotency-Key já foi usada")
    public ResponseEntity<TransferResponse> transfer(
            @Parameter(in = ParameterIn.HEADER, required = true,
                    description = "Chave única por transferência, ex.: um UUID")
            @RequestHeader(IDEMPOTENCY_HEADER) @NotBlank @Size(max = 64) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        var result = transferService.transfer(TransferMapper.toCommand(request, idempotencyKey));
        var status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(TransferMapper.toResponse(result.transfer()));
    }
}
