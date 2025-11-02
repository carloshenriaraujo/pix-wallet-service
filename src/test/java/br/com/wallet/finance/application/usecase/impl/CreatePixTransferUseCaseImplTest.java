package br.com.wallet.finance.application.usecase.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreatePixTransferUseCaseImplTest {

    private WalletRepository walletRepository;
    private PixKeyRepository pixKeyRepository;
    private PixTransferRepository pixTransferRepository;
    private LedgerEntryRepository ledgerEntryRepository;

    private CreatePixTransferUseCaseImpl useCase;

    @BeforeEach
    void setup() {
        walletRepository = mock(WalletRepository.class);
        pixKeyRepository = mock(PixKeyRepository.class);
        pixTransferRepository = mock(PixTransferRepository.class);
        ledgerEntryRepository = mock(LedgerEntryRepository.class);

        useCase = new CreatePixTransferUseCaseImpl(
                walletRepository,
                pixKeyRepository,
                pixTransferRepository,
                ledgerEntryRepository
        );
    }

    @Test
    void shouldReturnExistingTransferWhenIdempotencyMatch() {
        UUID fromWalletId = UUID.randomUUID();
        String idempotencyKey = "abc-idem";
        String toPixKeyValue = "user@pix.com";
        BigDecimal amount = new BigDecimal("50.00");

        Wallet mockFromWallet = Wallet.builder()
                .id(fromWalletId)
                .ownerName("Carlos")
                .currentBalance(new BigDecimal("1000.00"))
                .createdAt(Instant.now())
                .version(0L)
                .build();

        PixTransfer existing = PixTransfer.builder()
                .fromWallet(mockFromWallet) // <-- ESSENCIAL
                .endToEndId("e2e-123")
                .status(PixTransferStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .amount(amount)
                .build();

        when(pixTransferRepository.findByFromWallet_IdAndIdempotencyKey(fromWalletId, idempotencyKey))
                .thenReturn(Optional.of(existing));

        PixTransfer result = useCase.execute(idempotencyKey, fromWalletId, toPixKeyValue, amount);

        assertSame(existing, result);

        // garantimos que o resto do fluxo NÃO rodou
        verifyNoInteractions(walletRepository, pixKeyRepository, ledgerEntryRepository);
        verify(pixTransferRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenInsufficientFunds() {
        UUID fromWalletId = UUID.randomUUID();
        String idempotencyKey = "abc-idem";
        String toPixKeyValue = "user@pix.com";
        BigDecimal amount = new BigDecimal("200.00");

        Wallet fromWallet = Wallet.builder()
                .id(fromWalletId)
                .currentBalance(new BigDecimal("100.00"))
                .build();

        Wallet toWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .build();

        PixKey pixKey = PixKey.builder()
                .wallet(toWallet)
                .build();

        when(pixTransferRepository.findByFromWallet_IdAndIdempotencyKey(fromWalletId, idempotencyKey))
                .thenReturn(Optional.empty());

        when(walletRepository.findByIdForUpdate(fromWalletId))
                .thenReturn(Optional.of(fromWallet));

        when(pixKeyRepository.findByKeyValue(toPixKeyValue))
                .thenReturn(Optional.of(pixKey));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> useCase.execute(idempotencyKey, fromWalletId, toPixKeyValue, amount)
        );

        assertEquals("Insufficient funds for Pix transfer", ex.getMessage());

        verify(pixTransferRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
        verify(walletRepository, never()).save(fromWallet);
    }

    @Test
    void shouldCreatePendingTransferAndDebitSourceWallet() {
        UUID fromWalletId = UUID.randomUUID();
        UUID toWalletId = UUID.randomUUID();

        String idempotencyKey = "abc-idem";
        String toPixKeyValue = "user@pix.com";
        BigDecimal amount = new BigDecimal("75.00");

        Wallet fromWallet = Wallet.builder()
                .id(fromWalletId)
                .currentBalance(new BigDecimal("100.00"))
                .build();

        Wallet toWallet = Wallet.builder()
                .id(toWalletId)
                .currentBalance(new BigDecimal("0.00"))
                .build();

        PixKey toPixKey = PixKey.builder()
                .wallet(toWallet)
                .build();

        when(pixTransferRepository.findByFromWallet_IdAndIdempotencyKey(fromWalletId, idempotencyKey))
                .thenReturn(Optional.empty());

        when(walletRepository.findByIdForUpdate(fromWalletId))
                .thenReturn(Optional.of(fromWallet));

        when(pixKeyRepository.findByKeyValue(toPixKeyValue))
                .thenReturn(Optional.of(toPixKey));

        // precisamos simular o save do PixTransfer gerando um objeto "persistido"
        when(pixTransferRepository.save(any(PixTransfer.class))).thenAnswer(invocation -> {
            PixTransfer t = invocation.getArgument(0, PixTransfer.class);
            // simulando que o banco gerou um UUID
            t.setId(UUID.randomUUID());
            return t;
        });

        PixTransfer result = useCase.execute(idempotencyKey, fromWalletId, toPixKeyValue, amount);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(PixTransferStatus.PENDING, result.getStatus());
        assertEquals(amount, result.getAmount());
        assertEquals(idempotencyKey, result.getIdempotencyKey());
        assertEquals(toPixKeyValue, result.getToPixKey());
        assertEquals(fromWalletId, result.getFromWallet().getId());
        assertEquals(toWalletId, result.getToWallet().getId());
        assertNotNull(result.getEndToEndId());
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getUpdatedAt());

        assertEquals(new BigDecimal("25.00"), fromWallet.getCurrentBalance());

        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository).save(ledgerCaptor.capture());
        LedgerEntry savedEntry = ledgerCaptor.getValue();

        assertEquals(fromWallet, savedEntry.getWallet());
        assertEquals(amount, savedEntry.getAmount());
        assertEquals("PIX OUT PENDING", savedEntry.getDescription());
        assertNotNull(savedEntry.getEndToEndId());
        assertNotNull(savedEntry.getOccurredAt());

        verify(walletRepository).save(fromWallet);
    }

    @Test
    void shouldRejectTransferToSameWallet() {
        UUID walletId = UUID.randomUUID();
        String idempotencyKey = "idem";
        String toPixKeyValue = "me@me.com";
        BigDecimal amount = new BigDecimal("10.00");

        Wallet sameWallet = Wallet.builder()
                .id(walletId)
                .currentBalance(new BigDecimal("100.00"))
                .build();

        PixKey pixKey = PixKey.builder()
                .wallet(sameWallet)
                .build();

        when(pixTransferRepository.findByFromWallet_IdAndIdempotencyKey(walletId, idempotencyKey))
                .thenReturn(Optional.empty());

        when(walletRepository.findByIdForUpdate(walletId))
                .thenReturn(Optional.of(sameWallet));

        when(pixKeyRepository.findByKeyValue(toPixKeyValue))
                .thenReturn(Optional.of(pixKey));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> useCase.execute(idempotencyKey, walletId, toPixKeyValue, amount)
        );

        assertEquals("Cannot Pix transfer to same wallet", ex.getMessage());
        verify(pixTransferRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenIdempotencyKeyMissing() {
        UUID fromWalletId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10.00");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(null, fromWalletId, "abc", amount)
        );

        assertEquals("Missing Idempotency-Key header", ex.getMessage());
    }

    @Test
    void shouldFailWhenAmountIsInvalid() {
        UUID fromWalletId = UUID.randomUUID();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute("idem", fromWalletId, "abc", new BigDecimal("-1.00"))
        );

        assertEquals("Amount must be positive", ex.getMessage());
    }
}
