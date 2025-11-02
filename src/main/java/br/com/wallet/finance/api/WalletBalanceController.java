package br.com.wallet.finance.api;


import br.com.wallet.finance.api.dto.WalletBalanceApi;
import br.com.wallet.finance.api.dto.WalletCashFlowApi;
import br.com.wallet.finance.api.dto.request.DepositRequest;
import br.com.wallet.finance.api.dto.request.WithdrawRequest;
import br.com.wallet.finance.api.dto.response.BalanceResponse;
import br.com.wallet.finance.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
public class WalletBalanceController implements WalletBalanceApi, WalletCashFlowApi {

    private final GetBalanceUseCase getBalanceUseCase;
    private final GetHistoricalBalanceUseCase getHistoricalBalanceUseCase;
    private final DepositUseCase depositUseCase;
    private final WithdrawUseCase withdrawUseCase;

    public WalletBalanceController(
            GetBalanceUseCase getBalanceUseCase,
            GetHistoricalBalanceUseCase getHistoricalBalanceUseCase,
            DepositUseCase depositUseCase,
            WithdrawUseCase withdrawUseCase
    ) {
        this.getBalanceUseCase = getBalanceUseCase;
        this.getHistoricalBalanceUseCase = getHistoricalBalanceUseCase;
        this.depositUseCase = depositUseCase;
        this.withdrawUseCase = withdrawUseCase;
    }

    @Override
    public BalanceResponse getBalance(UUID walletId, Instant at) {
        BigDecimal balance = (at == null)
                ? getBalanceUseCase.execute(walletId)
                : getHistoricalBalanceUseCase.execute(walletId, at);

        return new BalanceResponse(walletId, balance);
    }

    @Override
    @ResponseStatus(HttpStatus.CREATED)
    public void deposit(UUID walletId, @Valid @RequestBody DepositRequest request) {
        depositUseCase.execute(walletId, request.amount(), request.description());
    }

    @Override
    @ResponseStatus(HttpStatus.CREATED)
    public void withdraw(UUID walletId, @Valid @RequestBody WithdrawRequest request) {
        withdrawUseCase.execute(walletId, request.amount(), request.description());
    }
}
