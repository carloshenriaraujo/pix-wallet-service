package br.com.wallet.finance.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "RegisterPixKeyResponse", description = "Informações da chave Pix registrada com sucesso")
public record RegisterPixKeyResponse(
        @Schema(description = "Identificador único da chave Pix", example = "b8bfb1b8-9b1a-4b0c-8fa6-cfa9c4e8af6b") UUID id,
        @Schema(description = "Identificador da carteira associada", example = "3e4a2cb2-47b8-40bd-9a85-984d6b71a7c0") UUID walletId,
        @Schema(description = "Tipo da chave Pix", example = "EMAIL") String keyType,
        @Schema(description = "Valor da chave Pix registrada", example = "jessica@example.com") String keyValue,
        @Schema(description = "Data de registro da chave", example = "2025-11-02T16:12:00Z") Instant createdAt
) {}