package br.com.wallet.finance.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(
        name = "WithdrawRequest",
        description = "Requisição para retirar saldo da carteira"
)
public record WithdrawRequest(

        @Schema(
                description = "Valor a debitar da carteira",
                example = "120.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull @Positive BigDecimal amount,

        @Schema(
                description = "Descrição do saque (ex: 'ATM withdraw', 'manual adjustment')",
                example = "ATM withdraw"
        )
        String description
) {}
