package br.com.wallet.finance.application.usecase;

import br.com.wallet.finance.domain.model.Wallet;

public interface CreateWalletUseCase {
    Wallet execute(String ownerName);
}
