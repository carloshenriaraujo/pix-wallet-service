package br.com.wallet.finance.application.usecase;

import br.com.wallet.finance.domain.model.PixKey;
import java.util.UUID;

public interface RegisterPixKeyUseCase {
    PixKey execute(UUID walletId, String keyType, String keyValue);
}
