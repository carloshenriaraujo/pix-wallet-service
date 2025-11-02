package br.com.wallet.finance.application.usecase.impl;

import br.com.wallet.finance.application.usecase.ProcessWebhookUseCase;
import br.com.wallet.finance.domain.enums.LedgerEntryType;
import br.com.wallet.finance.domain.enums.PixTransferStatus;
import br.com.wallet.finance.domain.exception.PixTransferNotFoundException;
import br.com.wallet.finance.domain.exception.WalletNotFoundException;
import br.com.wallet.finance.domain.model.LedgerEntry;
import br.com.wallet.finance.domain.model.PixTransfer;
import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.domain.model.WebhookEvent;
import br.com.wallet.finance.infrastructure.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ProcessWebhookUseCaseImpl implements ProcessWebhookUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessWebhookUseCaseImpl.class);

    private final WebhookEventRepository webhookEventRepository;
    private final PixTransferRepository pixTransferRepository;
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public ProcessWebhookUseCaseImpl(WebhookEventRepository webhookEventRepository, PixTransferRepository pixTransferRepository, WalletRepository walletRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.webhookEventRepository = webhookEventRepository;
        this.pixTransferRepository = pixTransferRepository;
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Override
    @Transactional
    public void execute(String eventId, String endToEndId, String eventType, Instant occurredAt) {

        if (isDuplicateEvent(eventId)) {
            log.info("pix_webhook_duplicate_ignored eventId={} endToEndId={} eventType={}", eventId, endToEndId, eventType);
            return;
        }

        PixTransfer transfer = loadTransfer(endToEndId);
        persistWebhookEvent(eventId, endToEndId, eventType, occurredAt);

        log.info("pix_webhook_received eventId={} endToEndId={} eventType={} currentStatus={}", eventId, endToEndId, eventType, transfer.getStatus());

        if (isConfirmed(eventType)) {
            handleConfirmed(transfer, eventId, endToEndId, eventType);
            return;
        }

        if (isRejected(eventType)) {
            handleRejected(transfer, eventId, endToEndId, eventType);
            return;
        }
        throw new IllegalArgumentException("Invalid eventType: " + eventType);
    }

    private void handleConfirmed(PixTransfer transfer, String eventId, String endToEndId, String eventType) {

        if (shouldIgnoreConfirmed(transfer)) {
            log.info("pix_webhook_confirm_ignored eventId={} endToEndId={} eventType={} reason={} finalStatus={}", eventId, endToEndId, eventType, "already_confirmed_or_rejected", transfer.getStatus());
            return;
        }

        Wallet toWallet = lockWalletForUpdate(transfer.getToWallet().getId());
        creditWallet(toWallet, transfer.getAmount(), transfer.getEndToEndId(), "PIX IN CONFIRMED");
        markTransferStatus(transfer, PixTransferStatus.CONFIRMED);

        log.info("pix_webhook_confirm_applied eventId={} endToEndId={} creditedWalletId={} amount={} newStatus={}", eventId, endToEndId, toWallet.getId(), transfer.getAmount(), transfer.getStatus());
    }

    private void handleRejected(PixTransfer transfer, String eventId, String endToEndId, String eventType) {

        if (shouldIgnoreRejected(transfer)) {
            log.info("pix_webhook_reject_ignored eventId={} endToEndId={} eventType={} reason={} finalStatus={}", eventId, endToEndId, eventType, "already_confirmed_or_rejected", transfer.getStatus());
            return;
        }

        Wallet fromWallet = lockWalletForUpdate(transfer.getFromWallet().getId());
        creditWallet(fromWallet, transfer.getAmount(), transfer.getEndToEndId(), "PIX REFUND REJECTED");
        markTransferStatus(transfer, PixTransferStatus.REJECTED);

        log.info("pix_webhook_reject_applied eventId={} endToEndId={} refundedWalletId={} amount={} newStatus={}", eventId, endToEndId, fromWallet.getId(), transfer.getAmount(), transfer.getStatus());
    }
    private boolean shouldIgnoreConfirmed(PixTransfer transfer) {
        PixTransferStatus status = transfer.getStatus();
        return status == PixTransferStatus.REJECTED || status == PixTransferStatus.CONFIRMED;
    }

    private boolean shouldIgnoreRejected(PixTransfer transfer) {
        PixTransferStatus status = transfer.getStatus();
        return status == PixTransferStatus.CONFIRMED || status == PixTransferStatus.REJECTED;
    }

    private void markTransferStatus(PixTransfer transfer, PixTransferStatus newStatus) {
        transfer.setStatus(newStatus);
        transfer.setUpdatedAt(Instant.now());
        pixTransferRepository.save(transfer);
    }

    private void creditWallet(Wallet wallet, java.math.BigDecimal amount, String endToEndId, String description) {

        LedgerEntry entry = LedgerEntry.builder().wallet(wallet).type(LedgerEntryType.CREDIT).amount(amount).endToEndId(endToEndId).description(description).occurredAt(Instant.now()).build();
        ledgerEntryRepository.save(entry);

        wallet.setCurrentBalance(wallet.getCurrentBalance().add(amount));
        walletRepository.save(wallet);
    }

    private boolean isDuplicateEvent(String eventId) {
        return webhookEventRepository.existsByEventId(eventId);
    }

    private PixTransfer loadTransfer(String endToEndId) {
        return pixTransferRepository.findByEndToEndId(endToEndId).orElseThrow(() -> new PixTransferNotFoundException("Pix transfer not found"));
    }

    private void persistWebhookEvent(String eventId, String endToEndId, String eventType, Instant occurredAt) {
        WebhookEvent event = WebhookEvent.builder().eventId(eventId).endToEndId(endToEndId).eventType(eventType).occurredAt(occurredAt != null ? occurredAt : Instant.now()).processedAt(Instant.now()).build();
        webhookEventRepository.save(event);
    }

    private boolean isConfirmed(String eventType) {
        return "CONFIRMED".equalsIgnoreCase(eventType);
    }

    private boolean isRejected(String eventType) {
        return "REJECTED".equalsIgnoreCase(eventType);
    }

    private Wallet lockWalletForUpdate(UUID walletId) {
        return walletRepository.findByIdForUpdate(walletId).orElseThrow(() -> new WalletNotFoundException("Wallet not found"));
    }
}