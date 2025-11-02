package br.com.wallet.finance.application.usecase.impl;

import br.com.wallet.finance.application.usecase.CreateWalletUseCase;
import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.infrastructure.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class CreateWalletUseCaseImpl implements CreateWalletUseCase {

    private final WalletRepository walletRepository;

    public CreateWalletUseCaseImpl(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    @Transactional
    public Wallet execute(String ownerName) {
        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("Owner name must not be null or blank");
        }
        Wallet wallet = Wallet.builder()
                .ownerName(ownerName)
                .currentBalance(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .version(0L)
                .build();

        return walletRepository.save(wallet);
    }
}
