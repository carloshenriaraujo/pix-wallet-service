package br.com.wallet.finance.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PixTransferResponse", description = "Resposta ao iniciar a transferência Pix")
public record PixTransferResponse(

        @Schema(
                description = "Identificador único Pix (endToEndId)",
                example = "8c2d3f28-39f0-499b-9b7f-2c4be609a5a1"
        )
        String endToEndId,

        @Schema(
                description = "Status atual da transferência",
                example = "PENDING"
        )
        String status
) {}