package br.com.wallet.finance.application.usecase.impl;

import br.com.wallet.finance.application.usecase.CreatePixTransferUseCase;
import br.com.wallet.finance.domain.enums.LedgerEntryType;
import br.com.wallet.finance.domain.enums.PixTransferStatus;
import br.com.wallet.finance.domain.exception.BusinessException;
import br.com.wallet.finance.domain.model.LedgerEntry;
import br.com.wallet.finance.domain.model.PixKey;
import br.com.wallet.finance.domain.model.PixTransfer;
import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.infrastructure.repository.LedgerEntryRepository;
import br.com.wallet.finance.infrastructure.repository.PixKeyRepository;
import br.com.wallet.finance.infrastructure.repository.PixTransferRepository;
import br.com.wallet.finance.infrastructure.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class CreatePixTransferUseCaseImpl implements CreatePixTransferUseCase {

    private static final Logger log = LoggerFactory.getLogger(CreatePixTransferUseCaseImpl.class);

    private final WalletRepository walletRepository;
    private final PixKeyRepository pixKeyRepository;
    private final PixTransferRepository pixTransferRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public CreatePixTransferUseCaseImpl(
            WalletRepository walletRepository,
            PixKeyRepository pixKeyRepository,
            PixTransferRepository pixTransferRepository,
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.walletRepository = walletRepository;
        this.pixKeyRepository = pixKeyRepository;
        this.pixTransferRepository = pixTransferRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Override
    @Transactional
    public PixTransfer execute(String idempotencyKey,
                               UUID fromWalletId,
                               String toPixKeyValue,
                               BigDecimal amount) {

        validateInput(idempotencyKey, amount);

        PixTransfer alreadyProcessed = findExistingTransfer(fromWalletId, idempotencyKey);
        if (alreadyProcessed != null) {
            log.info(
                    "pix_transfer_idempotent_detected endToEndId={} fromWalletId={} toWalletId={} amount={} idempotencyKey={} status={}",
                    alreadyProcessed.getEndToEndId(),
                    alreadyProcessed.getFromWallet().getId(),
                    alreadyProcessed.getToWallet() != null ? alreadyProcessed.getToWallet().getId() : null,
                    alreadyProcessed.getAmount(),
                    alreadyProcessed.getIdempotencyKey(),
                    alreadyProcessed.getStatus()
            );
            return alreadyProcessed;
        }

        Wallet fromWallet = loadSourceWalletLocked(fromWalletId);
        Wallet toWallet = resolveDestinationWallet(toPixKeyValue);

        validateWallets(fromWallet, toWallet);
        validateFunds(fromWallet, amount);

        String endToEndId = generateEndToEndId();

        PixTransfer transfer = savePendingTransfer(
                fromWallet, toWallet, toPixKeyValue, amount, endToEndId, idempotencyKey
        );

        log.info(
                "pix_transfer_created endToEndId={} fromWalletId={} toWalletId={} amount={} idempotencyKey={} status={}",
                endToEndId,
                fromWallet.getId(),
                toWallet.getId(),
                amount,
                idempotencyKey,
                transfer.getStatus()
        );

        registerDebitAndUpdateBalance(fromWallet, amount, endToEndId);

        log.info(
                "pix_transfer_debited endToEndId={} fromWalletId={} debitAmount={} newBalance={}",
                endToEndId,
                fromWallet.getId(),
                amount,
                fromWallet.getCurrentBalance()
        );

        return transfer;
    }

    private void validateInput(String idempotencyKey, BigDecimal amount) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Missing Idempotency-Key header");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private PixTransfer findExistingTransfer(UUID fromWalletId, String idempotencyKey) {
        return pixTransferRepository
                .findByFromWallet_IdAndIdempotencyKey(fromWalletId, idempotencyKey)
                .orElse(null);
    }

    private Wallet loadSourceWalletLocked(UUID fromWalletId) {
        return walletRepository.findByIdForUpdate(fromWalletId)
                .orElseThrow(() -> new IllegalArgumentException("From wallet not found"));
    }

    private Wallet resolveDestinationWallet(String toPixKeyValue) {
        PixKey toPixKey = pixKeyRepository.findByKeyValue(toPixKeyValue)
                .orElseThrow(() -> new IllegalArgumentException("Destination Pix key not found"));
        return toPixKey.getWallet();
    }

    private void validateWallets(Wallet fromWallet, Wallet toWallet) {
        if (fromWallet.getId().equals(toWallet.getId())) {
            throw new IllegalStateException("Cannot Pix transfer to same wallet");
        }
    }

    private void validateFunds(Wallet fromWallet, BigDecimal amount) {
        if (fromWallet.getCurrentBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient funds for Pix transfer");
        }
    }

    private String generateEndToEndId() {
        return UUID.randomUUID().toString();
    }

    private PixTransfer savePendingTransfer(Wallet fromWallet,
                                            Wallet toWallet,
                                            String toPixKeyValue,
                                            BigDecimal amount,
                                            String endToEndId,
                                            String idempotencyKey) {

        PixTransfer transfer = PixTransfer.builder()
                .fromWallet(fromWallet)
                .toWallet(toWallet)
                .toPixKey(toPixKeyValue)
                .amount(amount)
                .endToEndId(endToEndId)
                .idempotencyKey(idempotencyKey)
                .status(PixTransferStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        return pixTransferRepository.save(transfer);
    }

    private void registerDebitAndUpdateBalance(Wallet fromWallet,
                                               BigDecimal amount,
                                               String endToEndId) {

        LedgerEntry debitEntry = LedgerEntry.builder()
                .wallet(fromWallet)
                .type(LedgerEntryType.DEBIT)
                .amount(amount)
                .endToEndId(endToEndId)
                .description("PIX OUT PENDING")
                .occurredAt(Instant.now())
                .build();

        ledgerEntryRepository.save(debitEntry);

        fromWallet.setCurrentBalance(fromWallet.getCurrentBalance().subtract(amount));
        walletRepository.save(fromWallet);
    }
}