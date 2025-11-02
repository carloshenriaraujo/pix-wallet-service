package br.com.wallet.finance.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "CreateWalletResponse", description = "Informações retornadas após criação da carteira")
public record CreateWalletResponse(
        @Schema(description = "Identificador único da carteira", example = "6f7c29d1-8c2d-4a17-8f2a-f22c943f7b9c") UUID id,
        @Schema(description = "Nome do proprietário da carteira", example = "Carlos Henrique") String ownerName,
        @Schema(description = "Saldo inicial da carteira (sempre 0.00 na criação)", example = "0.00") BigDecimal currentBalance,
        @Schema(description = "Data e hora da criação (UTC)", example = "2025-11-02T16:10:02Z") Instant createdAt
) {}