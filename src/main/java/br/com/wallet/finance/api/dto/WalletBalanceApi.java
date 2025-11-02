package br.com.wallet.finance.api.dto;

import br.com.wallet.finance.api.dto.response.BalanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Tag(
        name = "Wallet Balance",
        description = "Consulta de saldo atual e saldo histórico da carteira"
)
@RequestMapping("/wallets")
public interface WalletBalanceApi {

    @Operation(
            summary = "Consulta saldo da carteira",
            description = """
                    Retorna o saldo da carteira.

                    • Sem parâmetro `at`: retorna o saldo atual (campo currentBalance da carteira).
                    
                    • Com parâmetro `at`: retorna o saldo histórico até aquele instante,
                      calculando todos os lançamentos de crédito e débito no ledger
                      com occurredAt <= at.

                    Exemplos:
                    
                    - Saldo atual:
                      GET /wallets/{walletId}/balance
                    
                    - Saldo histórico em 2025-10-09 15:00Z:
                      GET /wallets/{walletId}/balance?at=2025-10-09T15:00:00Z
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Saldo retornado com sucesso",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = BalanceResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Carteira não encontrada",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    @GetMapping("/{walletId}/balance")
    BalanceResponse getBalance(
            @Parameter(
                    name = "walletId",
                    in = ParameterIn.PATH,
                    required = true,
                    description = "ID da carteira que será consultada",
                    example = "6f7c29d1-8c2d-4a17-8f2a-f22c943f7b9c"
            )
            @PathVariable UUID walletId,

            @Parameter(
                    name = "at",
                    in = ParameterIn.QUERY,
                    required = false,
                    description = """
                            Momento no tempo (UTC) para cálculo de saldo histórico.
                            Formato ISO-8601.
                            Se não informado, retorna o saldo atual da carteira.
                            """,
                    example = "2025-10-09T15:00:00Z"
            )
            @RequestParam(name = "at", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant at
    );
}
