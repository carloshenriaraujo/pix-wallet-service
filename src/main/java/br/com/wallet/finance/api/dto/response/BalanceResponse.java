package br.com.wallet.finance.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(
        name = "BalanceResponse",
        description = "Representa o saldo atual ou histórico de uma carteira"
)
public record BalanceResponse(

        @Schema(
                description = "ID da carteira consultada",
                example = "6f7c29d1-8c2d-4a17-8f2a-f22c943f7b9c"
        )
        UUID walletId,

        @Schema(
                description = "Saldo da carteira no momento solicitado",
                example = "250.00"
        )
        BigDecimal balance
) {}