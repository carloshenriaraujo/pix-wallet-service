package br.com.wallet.finance.api.dto;


import br.com.wallet.finance.api.dto.request.DepositRequest;
import br.com.wallet.finance.api.dto.request.WithdrawRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(
        name = "Wallet Cash Flow",
        description = "Entradas e saídas de dinheiro da carteira (depósito e saque)"
)
@RequestMapping("/wallets")
public interface WalletCashFlowApi {

    @Operation(
            summary = "Realiza um depósito na carteira",
            description = """
                    Credita saldo na carteira informada.
                    
                    Regras:
                    - Gera um lançamento de CREDIT no ledger (auditoria).
                    - Atualiza o saldo atual da carteira de forma transacional.
                    
                    Exemplo de uso:
                    POST /wallets/{walletId}/deposit
                    {
                      "amount": 500.00,
                      "description": "Initial funding"
                    }
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Depósito aplicado com sucesso",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Carteira não encontrada",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Valor inválido (ex: negativo ou zero)",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @PostMapping("/{walletId}/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    void deposit(
            @Parameter(
                    name = "walletId",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "ID da carteira que receberá o crédito",
                    example = "6f7c29d1-8c2d-4a17-8f2a-f22c943f7b9c"
            )
            @PathVariable UUID walletId,

            @Valid @RequestBody DepositRequest request
    );


    @Operation(
            summary = "Realiza um saque da carteira",
            description = """
                    Debita saldo da carteira informada.
                    
                    Regras:
                    - Gera um lançamento de DEBIT no ledger (auditoria).
                    - Atualiza o saldo atual da carteira.
                    - Valida se há saldo suficiente.
                    
                    Exemplo de uso:
                    POST /wallets/{walletId}/withdraw
                    {
                      "amount": 120.00,
                      "description": "ATM withdraw"
                    }
                    
                    Possíveis erros:
                    - 409 Conflict -> saldo insuficiente.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Saque aplicado com sucesso",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Carteira não encontrada",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Valor inválido",
                            content = @Content(mediaType = "application/json")
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Saldo insuficiente",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @PostMapping("/{walletId}/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    void withdraw(
            @Parameter(
                    name = "walletId",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "ID da carteira que terá o débito",
                    example = "6f7c29d1-8c2d-4a17-8f2a-f22c943f7b9c"
            )
            @PathVariable UUID walletId,

            @Valid @RequestBody WithdrawRequest request
    );
}

