package br.com.wallet.finance.application.usecase;

import br.com.wallet.finance.domain.model.PixTransfer;

import java.math.BigDecimal;
import java.util.UUID;

public interface CreatePixTransferUseCase {

    PixTransfer execute(
            String idempotencyKey,
            UUID fromWalletId,
            String toPixKeyValue,
            BigDecimal amount
    );
}
