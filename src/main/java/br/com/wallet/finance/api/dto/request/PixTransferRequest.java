package br.com.wallet.finance.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "PixTransferRequest", description = "Dados para iniciar uma transferência Pix interna")
public record PixTransferRequest(

        @Schema(
                description = "Carteira de origem (quem está enviando o Pix)",
                example = "6f7c29d1-8c2d-4a17-8f2a-f22c943f7b9c",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        UUID fromWalletId,

        @Schema(
                description = "Chave Pix de destino (email, telefone ou EVP)",
                example = "jessica@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull
        String toPixKey,

        @Schema(
                description = "Valor a ser transferido",
                example = "150.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull @Positive
        BigDecimal amount
) {}