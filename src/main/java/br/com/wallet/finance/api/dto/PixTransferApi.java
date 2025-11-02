package br.com.wallet.finance.api.dto;

import br.com.wallet.finance.api.dto.request.PixTransferRequest;
import br.com.wallet.finance.api.dto.response.PixTransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Pix Transfers",
        description = "Inicia transferências Pix internas com idempotência e consistência transacional"
)
@RequestMapping("/pix")
public interface PixTransferApi {

    @Operation(
            summary = "Inicia uma transferência Pix",
            description = """
                    Cria uma transferência Pix interna entre carteiras do sistema.
                    
                    - Debita a carteira de origem imediatamente.
                    - Marca a transferência como PENDING.
                    - NÃO credita ainda o destinatário (o crédito vem no webhook /pix/webhook).
                    
                    A chamada é idempotente via header `Idempotency-Key`: 
                    se a mesma requisição for reenviada com a mesma chave, 
                    o serviço retorna a mesma transferência sem debitar de novo.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Transferência criada com sucesso",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = PixTransferResponse.class)
                            ),
                            headers = {
                                    @Header(
                                            name = "Idempotency-Key",
                                            description = "Chave de idempotência recebida na requisição original. Se repetir, o débito não é duplicado.",
                                            schema = @Schema(type = "string", example = "d1ce3b1f-6a0d-4d42-90d9-7bea44f3c9f3")
                                    )
                            }
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Requisição inválida (ex: valor negativo ou header ausente)",
                            content = @Content(
                                    mediaType = "application/json"
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Saldo insuficiente ou tentativa de transferir para si mesmo",
                            content = @Content(
                                    mediaType = "application/json"
                            )
                    )
            }
    )
    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    PixTransferResponse createTransfer(
            @Parameter(
                    name = "Idempotency-Key",
                    in = ParameterIn.HEADER,
                    required = true,
                    description = "Chave única enviada pelo cliente para garantir efeito 'exactly-once'.\n" +
                            "Se o cliente reenviar a mesma operação com a mesma chave, " +
                            "a API devolve o mesmo endToEndId e status, sem debitar de novo.",
                    example = "d1ce3b1f-6a0d-4d42-90d9-7bea44f3c9f3"
            )
            @RequestHeader("Idempotency-Key") String idempotencyKey,

            @Valid @RequestBody PixTransferRequest request
    );
}