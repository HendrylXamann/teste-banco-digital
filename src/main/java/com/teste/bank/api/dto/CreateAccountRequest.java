package com.teste.bank.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record CreateAccountRequest(
        @Schema(example = "Daniel")
        @NotBlank @Size(max = 120) String name,
        @Schema(example = "250.00")
        @NotNull @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal initialBalance) {
}
