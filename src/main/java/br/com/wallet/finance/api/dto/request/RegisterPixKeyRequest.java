package br.com.wallet.finance.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(name = "RegisterPixKeyRequest", description = "Requisição para registrar uma chave Pix única")
public record RegisterPixKeyRequest(
        @Schema(description = "Tipo da chave Pix (EMAIL, PHONE, EVP, CPF, CNPJ)", example = "EMAIL", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull String keyType,

        @Schema(description = "Valor da chave Pix (deve ser único no sistema)", example = "jessica@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String keyValue
) {}