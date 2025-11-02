package br.com.wallet.finance.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

public interface GetBalanceUseCase {
    BigDecimal execute(UUID walletId);
}
