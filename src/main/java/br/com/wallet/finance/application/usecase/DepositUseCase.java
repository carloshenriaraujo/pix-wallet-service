package br.com.wallet.finance.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

public interface DepositUseCase {
    void execute(UUID walletId, BigDecimal amount, String description);
}
