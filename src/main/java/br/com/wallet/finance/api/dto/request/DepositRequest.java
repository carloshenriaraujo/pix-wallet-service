package br.com.wallet.finance.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(
        name = "DepositRequest",
        description = "Requisição para adicionar saldo na carteira"
)
public record DepositRequest(

        @Schema(
                description = "Valor a creditar na carteira",
                example = "500.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull @Positive BigDecimal amount,

        @Schema(
                description = "Descrição ou origem do depósito (opcional, só pra auditoria)",
                example = "Initial funding"
        )
        String description
) {}
