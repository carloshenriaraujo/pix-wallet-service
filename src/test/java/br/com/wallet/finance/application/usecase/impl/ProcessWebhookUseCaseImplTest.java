package br.com.wallet.finance.application.usecase.impl;

import br.com.wallet.finance.domain.enums.LedgerEntryType;
import br.com.wallet.finance.domain.enums.PixTransferStatus;
import br.com.wallet.finance.domain.exception.PixTransferNotFoundException;
import br.com.wallet.finance.domain.model.LedgerEntry;
import br.com.wallet.finance.domain.model.PixTransfer;
import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.domain.model.WebhookEvent;
import br.com.wallet.finance.infrastructure.repository.LedgerEntryRepository;
import br.com.wallet.finance.infrastructure.repository.PixTransferRepository;
import br.com.wallet.finance.infrastructure.repository.WalletRepository;
import br.com.wallet.finance.infrastructure.repository.WebhookEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ProcessWebhookUseCaseImplTest {

    private WebhookEventRepository webhookEventRepository;
    private PixTransferRepository pixTransferRepository;
    private WalletRepository walletRepository;
    private LedgerEntryRepository ledgerEntryRepository;

    private ProcessWebhookUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        webhookEventRepository = mock(WebhookEventRepository.class);
        pixTransferRepository = mock(PixTransferRepository.class);
        walletRepository = mock(WalletRepository.class);
        ledgerEntryRepository = mock(LedgerEntryRepository.class);

        useCase = new ProcessWebhookUseCaseImpl(
                webhookEventRepository,
                pixTransferRepository,
                walletRepository,
                ledgerEntryRepository
        );
    }

    @Test
    void should_confirm_pending_transfer_and_credit_destination_wallet() {
        // Given (arranjo)

        String eventId = "evt-123";
        String endToEndId = "e2e-abc";
        Instant occurredAt = Instant.parse("2025-10-09T15:00:00Z");

        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();

        // Wallet de destino antes do crédito
        Wallet toWallet = Wallet.builder()
                .id(toWalletId)
                .ownerName("Destinatario")
                .currentBalance(new BigDecimal("50.00"))
                .createdAt(Instant.now())
                .version(0L)
                .build();

        // Transferência PENDING antes do webhook
        PixTransfer transfer = PixTransfer.builder()
                .fromWallet(Wallet.builder().id(fromWalletId).build())
                .toWallet(Wallet.builder().id(toWalletId).build())
                .amount(new BigDecimal("30.00"))
                .endToEndId(endToEndId)
                .status(PixTransferStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        // Stubs dos repositórios
        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(pixTransferRepository.findByEndToEndId(endToEndId)).thenReturn(Optional.of(transfer));
        when(walletRepository.findByIdForUpdate(toWalletId)).thenReturn(Optional.of(toWallet));

        // When (ação)
        useCase.execute(
                eventId,
                endToEndId,
                "CONFIRMED",
                occurredAt
        );

        // Then (verificações)

        // 1. Deve salvar um WebhookEvent com os dados corretos
        ArgumentCaptor<WebhookEvent> webhookCaptor = ArgumentCaptor.forClass(WebhookEvent.class);
        verify(webhookEventRepository).save(webhookCaptor.capture());
        WebhookEvent savedEvent = webhookCaptor.getValue();

        assertEquals(eventId, savedEvent.getEventId());
        assertEquals(endToEndId, savedEvent.getEndToEndId());
        assertEquals("CONFIRMED", savedEvent.getEventType());
        assertEquals(occurredAt, savedEvent.getOccurredAt());
        assertNotNull(savedEvent.getProcessedAt(), "processedAt deve ser preenchido");

        // 2. Deve criar uma LedgerEntry de crédito na carteira destino
        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(ledgerCaptor.capture());
        LedgerEntry savedLedger = ledgerCaptor.getValue();

        assertEquals(toWallet, savedLedger.getWallet());
        assertEquals(LedgerEntryType.CREDIT, savedLedger.getType());
        assertEquals(new BigDecimal("30.00"), savedLedger.getAmount());
        assertEquals(endToEndId, savedLedger.getEndToEndId());
        assertEquals("PIX IN CONFIRMED", savedLedger.getDescription());
        assertNotNull(savedLedger.getOccurredAt(), "occurredAt deve ser preenchido");

        // 3. Deve atualizar o saldo da carteira destino
        // Antes: 50.00, valor Pix: 30.00 → Depois: 80.00
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet updatedWallet = walletCaptor.getValue();

        assertEquals(new BigDecimal("80.00"), updatedWallet.getCurrentBalance());

        // 4. Deve marcar a transferência como CONFIRMED e salvar
        ArgumentCaptor<PixTransfer> transferCaptor = ArgumentCaptor.forClass(PixTransfer.class);
        verify(pixTransferRepository).save(transferCaptor.capture());
        PixTransfer updatedTransfer = transferCaptor.getValue();

        assertEquals(PixTransferStatus.CONFIRMED, updatedTransfer.getStatus());
        assertNotNull(updatedTransfer.getUpdatedAt(), "updatedAt deve ser atualizado");

        // 5. Não deve tentar estornar origem nesse fluxo
        // (garantir que não chamou lock da origem nem mexeu com DEBIT/CREDIT errado)
        verify(walletRepository, never()).findByIdForUpdate(fromWalletId);
    }

    @Test
    void should_ignore_duplicate_event() {
        // Given
        String eventId = "evt-dup";
        String endToEndId = "e2e-dup";

        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(true);

        // When
        useCase.execute(
                eventId,
                endToEndId,
                "CONFIRMED",
                Instant.now()
        );

        // Then
        // não deve carregar transferência
        verify(pixTransferRepository, never()).findByEndToEndId(anyString());

        // não deve salvar ledger
        verify(ledgerEntryRepository, never()).save(any());

        // não deve salvar wallet
        verify(walletRepository, never()).save(any());

        // pode ou não salvar webhookEvent? -> No nosso código atual:
        // se já existe o eventId, ele sai antes de persistir qualquer coisa.
        verify(webhookEventRepository, never()).save(any());
    }

    @Test
    void should_reject_pending_transfer_and_refund_source_wallet() {
        // Cenário REJECTED:
        // - transferência está PENDING
        // - evento é REJECTED
        // - precisa devolver o dinheiro pra carteira origem

        String eventId = "evt-reject-1";
        String endToEndId = "e2e-r1";
        Instant occurredAt = Instant.parse("2025-10-09T15:00:00Z");

        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId   = UUID.randomUUID();

        Wallet fromWallet = Wallet.builder()
                .id(fromWalletId)
                .ownerName("Origem")
                .currentBalance(new BigDecimal("100.00"))
                .createdAt(Instant.now())
                .version(0L)
                .build();

        PixTransfer transfer = PixTransfer.builder()
                .fromWallet(Wallet.builder().id(fromWalletId).build())
                .toWallet(Wallet.builder().id(toWalletId).build())
                .amount(new BigDecimal("40.00"))
                .endToEndId(endToEndId)
                .status(PixTransferStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(pixTransferRepository.findByEndToEndId(endToEndId)).thenReturn(Optional.of(transfer));
        when(walletRepository.findByIdForUpdate(fromWalletId)).thenReturn(Optional.of(fromWallet));

        // ação
        useCase.execute(
                eventId,
                endToEndId,
                "REJECTED",
                occurredAt
        );

        // valida que o evento foi persistido
        verify(webhookEventRepository).save(any(WebhookEvent.class));

        // captura ledger criado (estorno)
        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(ledgerCaptor.capture());
        LedgerEntry refundLedger = ledgerCaptor.getValue();

        assertEquals(LedgerEntryType.CREDIT, refundLedger.getType(), "estorno deve ser CREDIT na origem");
        assertEquals(new BigDecimal("40.00"), refundLedger.getAmount());
        assertEquals("PIX REFUND REJECTED", refundLedger.getDescription());
        assertEquals(endToEndId, refundLedger.getEndToEndId());
        assertNotNull(refundLedger.getOccurredAt());

        // saldo da origem deve aumentar 40.00: 100.00 + 40.00 = 140.00
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet updatedFromWallet = walletCaptor.getValue();
        assertEquals(new BigDecimal("140.00"), updatedFromWallet.getCurrentBalance());

        // status da transferência deve virar REJECTED
        ArgumentCaptor<PixTransfer> transferCaptor = ArgumentCaptor.forClass(PixTransfer.class);
        verify(pixTransferRepository).save(transferCaptor.capture());
        PixTransfer updatedTransfer = transferCaptor.getValue();
        assertEquals(PixTransferStatus.REJECTED, updatedTransfer.getStatus());
        assertNotNull(updatedTransfer.getUpdatedAt());

        // destino nunca é travado nesse caso
        verify(walletRepository, never()).findByIdForUpdate(toWalletId);
    }

    @Test
    void should_throw_if_invalid_event_type() {
        String eventId = "evt-x";
        String endToEndId = "e2e-x";

        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);

        PixTransfer transfer = PixTransfer.builder()
                .status(PixTransferStatus.PENDING)
                .endToEndId(endToEndId)
                .amount(new BigDecimal("10.00"))
                .fromWallet(Wallet.builder().id(UUID.randomUUID()).build())
                .toWallet(Wallet.builder().id(UUID.randomUUID()).build())
                .build();

        when(pixTransferRepository.findByEndToEndId(endToEndId))
                .thenReturn(Optional.of(transfer));

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(eventId, endToEndId, "WHAT_IS_THIS", Instant.now())
        );

        // não deve creditar/estornar ninguém
        verify(walletRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }
    @Test
    void should_throw_if_transfer_not_found() {
        // Given
        String eventId = "evt-missing-transfer";
        String endToEndId = "e2e-does-not-exist";

        // Simula: esse eventId ainda não foi processado
        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);

        // Simula: nenhuma transferência com esse endToEndId
        when(pixTransferRepository.findByEndToEndId(endToEndId))
                .thenReturn(Optional.empty());

        // When + Then
        assertThrows(
                PixTransferNotFoundException.class,
                () -> useCase.execute(
                        eventId,
                        endToEndId,
                        "CONFIRMED",
                        Instant.now()
                ),
                "Quando a transferência não existe, deve lançar PixTransferNotFoundException"
        );

        // E não deve salvar evento de webhook (porque falhou antes)
        verify(webhookEventRepository, never()).save(any(WebhookEvent.class));

        // E não deve mexer em carteiras nem ledger
        verify(walletRepository, never()).findByIdForUpdate(any());
        verify(walletRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }
}