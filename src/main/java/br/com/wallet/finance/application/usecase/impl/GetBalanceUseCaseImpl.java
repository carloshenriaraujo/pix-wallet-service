package br.com.wallet.finance.application.usecase.impl;

import br.com.wallet.finance.application.usecase.GetBalanceUseCase;
import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.infrastructure.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class GetBalanceUseCaseImpl implements GetBalanceUseCase {

    private final WalletRepository walletRepository;

    public GetBalanceUseCaseImpl(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal execute(UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        return wallet.getCurrentBalance();
    }
}
