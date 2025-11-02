package br.com.wallet.finance.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "CreateWalletRequest", description = "Dados necessários para criar uma nova carteira")
public record CreateWalletRequest(
        @Schema(description = "Nome do proprietário da carteira", example = "Carlos Henrique", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String ownerName
) {}
