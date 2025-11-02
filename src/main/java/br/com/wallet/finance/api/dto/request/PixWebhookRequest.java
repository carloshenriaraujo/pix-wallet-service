package br.com.wallet.finance.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Schema(
        name = "PixWebhookRequest",
        description = """
                Evento de confirmação de Pix simulando o arranjo externo (ex: Bacen).
                
                A aplicação trata esse webhook como fonte de verdade sobre o resultado do Pix:
                - CONFIRMED: o dinheiro é creditado na carteira destino.
                - REJECTED: o valor é estornado para a carteira origem.
                
                A mesma notificação pode chegar mais de uma vez ou fora de ordem.
                O campo eventId garante idempotência.
                """
)
public record PixWebhookRequest(

        @Schema(
                description = "Identificador único do evento enviado pelo sistema externo. Usado para garantir idempotência.",
                example = "evt-111"
        )
        @NotBlank String eventId,

        @Schema(
                description = "Identificador end-to-end do Pix. Esse valor vem da criação /pix/transfers.",
                example = "8c2d3f28-39f0-499b-9b7f-2c4be609a5a1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank String endToEndId,

        @Schema(
                description = "Tipo do evento recebido. Pode ser CONFIRMED ou REJECTED.",
                example = "CONFIRMED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank String eventType,

        @Schema(
                description = "Momento em que o evento aconteceu no provedor externo (UTC).",
                example = "2025-11-02T18:00:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull Instant occurredAt
) {}
