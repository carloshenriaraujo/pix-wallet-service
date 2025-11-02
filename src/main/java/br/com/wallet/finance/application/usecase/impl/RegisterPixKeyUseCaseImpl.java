package br.com.wallet.finance.application.usecase.impl;

import br.com.wallet.finance.application.usecase.RegisterPixKeyUseCase;
import br.com.wallet.finance.domain.exception.PixKeyAlreadyExistsException;
import br.com.wallet.finance.domain.exception.WalletNotFoundException;
import br.com.wallet.finance.domain.model.PixKey;
import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.infrastructure.repository.PixKeyRepository;
import br.com.wallet.finance.infrastructure.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RegisterPixKeyUseCaseImpl implements RegisterPixKeyUseCase {

    private final WalletRepository walletRepository;
    private final PixKeyRepository pixKeyRepository;

    public RegisterPixKeyUseCaseImpl(WalletRepository walletRepository,
                                     PixKeyRepository pixKeyRepository) {
        this.walletRepository = walletRepository;
        this.pixKeyRepository = pixKeyRepository;
    }

    @Override
    @Transactional
    public PixKey execute(UUID walletId, String keyType, String keyValue) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));

        if (pixKeyRepository.existsByKeyValue(keyValue)) {
            throw new PixKeyAlreadyExistsException("Pix key already registered");
        }

        PixKey pixKey = PixKey.builder()
                .wallet(wallet)
                .keyType(keyType)
                .keyValue(keyValue)
                .createdAt(Instant.now())
                .build();

        return pixKeyRepository.save(pixKey);
    }
}
