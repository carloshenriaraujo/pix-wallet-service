package br.com.wallet.finance.api.dto;

import br.com.wallet.finance.api.dto.request.PixWebhookRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Pix Webhook",
        description = "Processa eventos externos do Pix (CONFIRMED / REJECTED) com idempotência e consistência"
)
@RequestMapping("/pix")
public interface PixWebhookApi {

    @Operation(
            summary = "Processa um evento Pix CONFIRMED ou REJECTED",
            description = """
                    Esse endpoint simula o webhook vindo da infraestrutura Pix.
                    
                    - CONFIRMED:
                      • Credita a carteira destino.
                      • Atualiza a transferência para CONFIRMED.
                      • Gera um lançamento 'PIX IN CONFIRMED' no ledger da carteira destino.
                      
                    - REJECTED:
                      • Estorna o valor para a carteira origem.
                      • Atualiza a transferência para REJECTED.
                      • Gera um lançamento 'PIX REFUND REJECTED' no ledger da carteira origem.
                    
                    Requisitos não funcionais atendidos:
                    
                    • Idempotência:
                      O campo `eventId` é armazenado. Se o mesmo evento chegar de novo,
                      o sistema ignora para não aplicar crédito/estorno duas vezes.
                    
                    • Ordem trocada:
                      Se um REJECTED chegar antes de um CONFIRMED, o sistema marca REJECTED
                      e ignora CONFIRMED tardio. E vice-versa.
                    
                    • Concorrência:
                      O saldo das carteiras envolvidas é atualizado transacionalmente
                      com travas para evitar corrida.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "202",
                            description = "Evento aceito e processado (ou reconhecido como repetido/idempotente)",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Requisição inválida",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Transferência Pix não encontrada para o endToEndId informado",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void handleWebhook(
            @Valid @RequestBody
            @Schema(
                    description = "Payload do evento Pix recebido do arranjo",
                    implementation = PixWebhookRequest.class
            )
            PixWebhookRequest request
    );
}

