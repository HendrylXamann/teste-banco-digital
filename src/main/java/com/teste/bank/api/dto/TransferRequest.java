package com.teste.bank.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record TransferRequest(
        @Schema(example = "1")
        @NotNull Long fromAccountId,
        @Schema(example = "2")
        @NotNull Long toAccountId,
        @Schema(example = "100.00")
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount) {
}
