package br.com.wallet.finance.application.usecase.impl;

import br.com.wallet.finance.domain.enums.LedgerEntryType;
import br.com.wallet.finance.domain.model.LedgerEntry;
import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.infrastructure.repository.LedgerEntryRepository;
import br.com.wallet.finance.infrastructure.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetHistoricalBalanceUseCaseImplTest {

    private WalletRepository walletRepository;
    private LedgerEntryRepository ledgerEntryRepository;
    private GetHistoricalBalanceUseCaseImpl useCase;

    @BeforeEach
    void setup() {
        walletRepository = mock(WalletRepository.class);
        ledgerEntryRepository = mock(LedgerEntryRepository.class);
        useCase = new GetHistoricalBalanceUseCaseImpl(walletRepository, ledgerEntryRepository);
    }

    @Test
    void shouldCalculateHistoricalBalanceBasedOnLedgerEntries() {
        // given
        UUID walletId = UUID.randomUUID();
        Instant atInstant = Instant.parse("2025-10-09T15:00:00Z");

        // a carteira existe
        Wallet wallet = Wallet.builder()
                .id(walletId)
                .ownerName("Carlos")
                .currentBalance(new BigDecimal("500.00"))
                .createdAt(Instant.now())
                .version(0L)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        // ledger_entries até aquele timestamp
        LedgerEntry credit100 = LedgerEntry.builder()
                .wallet(wallet)
                .type(LedgerEntryType.CREDIT)
                .amount(new BigDecimal("100.00"))
                .occurredAt(Instant.parse("2025-10-09T14:00:00Z"))
                .build();

        LedgerEntry debit30 = LedgerEntry.builder()
                .wallet(wallet)
                .type(LedgerEntryType.DEBIT)
                .amount(new BigDecimal("30.00"))
                .occurredAt(Instant.parse("2025-10-09T14:30:00Z"))
                .build();

        LedgerEntry credit10 = LedgerEntry.builder()
                .wallet(wallet)
                .type(LedgerEntryType.CREDIT)
                .amount(new BigDecimal("10.00"))
                .occurredAt(Instant.parse("2025-10-09T15:00:00Z"))
                .build();

        List<LedgerEntry> ledgerEntries = Arrays.asList(
                credit100,
                debit30,
                credit10
        );

        when(ledgerEntryRepository.findByWallet_IdAndOccurredAtLessThanEqual(walletId, atInstant))
                .thenReturn(ledgerEntries);

        // when
        BigDecimal result = useCase.execute(walletId, atInstant);

        // then
        // saldo esperado = +100.00 -30.00 +10.00 = 80.00
        assertEquals(new BigDecimal("80.00"), result);

        // garante que buscamos a carteira e as entradas corretas
        verify(walletRepository).findById(walletId);
        verify(ledgerEntryRepository).findByWallet_IdAndOccurredAtLessThanEqual(walletId, atInstant);
        verifyNoMoreInteractions(walletRepository, ledgerEntryRepository);
    }

    @Test
    void shouldThrowIfWalletDoesNotExist() {
        // given
        UUID walletId = UUID.randomUUID();
        Instant atInstant = Instant.parse("2025-10-09T15:00:00Z");

        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // when / then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(walletId, atInstant)
        );

        assertEquals("Wallet not found", ex.getMessage());

        // ledger NUNCA deve ser consultado se a carteira não existe
        verify(walletRepository).findById(walletId);
        verify(ledgerEntryRepository, never())
                .findByWallet_IdAndOccurredAtLessThanEqual(
                        ArgumentMatchers.any(), ArgumentMatchers.any()
                );
        verifyNoMoreInteractions(walletRepository, ledgerEntryRepository);
    }
}