package br.com.wallet.finance.application.usecase.impl;

import br.com.wallet.finance.application.usecase.GetHistoricalBalanceUseCase;
import br.com.wallet.finance.domain.enums.LedgerEntryType;
import br.com.wallet.finance.domain.model.LedgerEntry;
import br.com.wallet.finance.infrastructure.repository.LedgerEntryRepository;
import br.com.wallet.finance.infrastructure.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class GetHistoricalBalanceUseCaseImpl implements GetHistoricalBalanceUseCase {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public GetHistoricalBalanceUseCaseImpl(
            WalletRepository walletRepository,
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal execute(UUID walletId, Instant atInstant) {
        // garante que a wallet existe (se não existir -> 404)
        walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        List<LedgerEntry> entries =
                ledgerEntryRepository.findByWallet_IdAndOccurredAtLessThanEqual(walletId, atInstant);

        BigDecimal balance = BigDecimal.ZERO;

        for (LedgerEntry e : entries) {
            if (e.getType() == LedgerEntryType.CREDIT) {
                balance = balance.add(e.getAmount());
            } else if (e.getType() == LedgerEntryType.DEBIT) {
                balance = balance.subtract(e.getAmount());
            }
        }

        return balance;
    }
}
