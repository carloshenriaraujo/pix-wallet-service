package br.com.wallet.finance.application.usecase.impl;

import br.com.wallet.finance.application.usecase.DepositUseCase;
import br.com.wallet.finance.domain.enums.LedgerEntryType;
import br.com.wallet.finance.domain.exception.WalletNotFoundException;
import br.com.wallet.finance.domain.model.LedgerEntry;
import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.infrastructure.repository.LedgerEntryRepository;
import br.com.wallet.finance.infrastructure.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class DepositUseCaseImpl implements DepositUseCase {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public DepositUseCaseImpl(
            WalletRepository walletRepository,
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Override
    @Transactional
    public void execute(UUID walletId, BigDecimal amount, String description) {
        validateAmount(amount);

        Wallet wallet = getWalletLocked(walletId);

        createLedgerEntry(wallet, amount, description);
        updateWalletBalance(wallet, amount);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private Wallet getWalletLocked(UUID walletId) {
        return walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
    }

    private void createLedgerEntry(Wallet wallet, BigDecimal amount, String description) {
        LedgerEntry entry = LedgerEntry.builder()
                .wallet(wallet)
                .type(LedgerEntryType.CREDIT)
                .amount(amount)
                .description(getDescriptionOrDefault(description))
                .occurredAt(Instant.now())
                .build();

        ledgerEntryRepository.save(entry);
    }

    private void updateWalletBalance(Wallet wallet, BigDecimal amount) {
        wallet.setCurrentBalance(wallet.getCurrentBalance().add(amount));
        walletRepository.save(wallet);
    }

    private String getDescriptionOrDefault(String description) {
        return (description == null || description.isBlank()) ? "DEPOSIT" : description;
    }
}