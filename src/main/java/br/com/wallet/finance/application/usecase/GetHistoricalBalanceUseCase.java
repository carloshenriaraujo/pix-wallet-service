package br.com.wallet.finance.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface GetHistoricalBalanceUseCase {
    BigDecimal execute(UUID walletId, Instant atInstant);
}
