package br.com.wallet.finance.api.dto;

import br.com.wallet.finance.api.dto.request.CreateWalletRequest;
import br.com.wallet.finance.api.dto.response.CreateWalletResponse;
import br.com.wallet.finance.api.dto.request.RegisterPixKeyRequest;
import br.com.wallet.finance.api.dto.response.RegisterPixKeyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Wallet Management", description = "Gerenciamento de carteiras e chaves Pix")
@RequestMapping("/wallets")
public interface WalletApi {

    @Operation(
            summary = "Cria uma nova carteira",
            description = """
                    Cria uma carteira digital para um novo usuário.
                    A carteira é inicializada com saldo 0.00 e pode posteriormente
                    receber depósitos e registrar chaves Pix.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Carteira criada com sucesso",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = CreateWalletResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content)
            }
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreateWalletResponse createWallet(
            @Valid @RequestBody CreateWalletRequest req
    );

    @Operation(
            summary = "Registra uma chave Pix para a carteira",
            description = """
                    Vincula uma nova chave Pix a uma carteira.
                    Cada chave Pix deve ser única no sistema.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Chave Pix registrada com sucesso",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = RegisterPixKeyResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Requisição inválida", content = @Content),
                    @ApiResponse(responseCode = "409", description = "Chave Pix já registrada", content = @Content)
            }
    )
    @PostMapping("/{walletId}/pix-keys")
    @ResponseStatus(HttpStatus.CREATED)
    RegisterPixKeyResponse registerPixKey(
            @Parameter(name = "walletId", in = ParameterIn.PATH, description = "ID da carteira", required = true,
                    example = "3e4a2cb2-47b8-40bd-9a85-984d6b71a7c0")
            @PathVariable UUID walletId,
            @Valid @RequestBody RegisterPixKeyRequest req
    );
}
