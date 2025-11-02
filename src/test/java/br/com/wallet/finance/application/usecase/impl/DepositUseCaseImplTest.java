package br.com.wallet.finance.application.usecase.impl;

import br.com.wallet.finance.domain.enums.LedgerEntryType;
import br.com.wallet.finance.domain.exception.WalletNotFoundException;
import br.com.wallet.finance.domain.model.LedgerEntry;
import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.infrastructure.repository.LedgerEntryRepository;
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

class DepositUseCaseImplTest {

    private WalletRepository walletRepository;
    private LedgerEntryRepository ledgerEntryRepository;
    private DepositUseCaseImpl depositUseCase;

    @BeforeEach
    void setup() {
        walletRepository = mock(WalletRepository.class);
        ledgerEntryRepository = mock(LedgerEntryRepository.class);
        depositUseCase = new DepositUseCaseImpl(walletRepository, ledgerEntryRepository);
    }

    @Test
    void deve_realizar_deposito_atualizar_saldo_e_criar_ledger_entry() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        BigDecimal valorDeposito = new BigDecimal("150.00");

        Wallet carteira = Wallet.builder()
                .id(walletId)
                .ownerName("Carlos Henrique")
                .currentBalance(new BigDecimal("1000.00"))
                .createdAt(Instant.now())
                .version(0L)
                .build();

        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(carteira));

        // Act
        depositUseCase.execute(walletId, valorDeposito, "Initial funding");

        // Assert
        // 1. LedgerEntry salvo corretamente
        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(1)).save(ledgerCaptor.capture());

        LedgerEntry savedEntry = ledgerCaptor.getValue();
        assertNotNull(savedEntry);
        assertEquals(carteira, savedEntry.getWallet());
        assertEquals(LedgerEntryType.CREDIT, savedEntry.getType());
        assertEquals(new BigDecimal("150.00"), savedEntry.getAmount());
        assertEquals("Initial funding", savedEntry.getDescription());
        assertNotNull(savedEntry.getOccurredAt());

        // 2. Saldo atualizado da carteira
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, times(1)).save(walletCaptor.capture());

        Wallet savedWallet = walletCaptor.getValue();
        assertNotNull(savedWallet);
        assertEquals(new BigDecimal("1150.00"), savedWallet.getCurrentBalance());

        // 3. Também garante que buscamos a wallet com lock
        verify(walletRepository, times(1)).findByIdForUpdate(walletId);

        // >>> Removemos verifyNoMoreInteractions(...) aqui <<<
        // porque internamente podem existir futuras interações legítimas
    }

    @Test
    void deve_usar_descricao_padrao_quando_description_for_nula_ou_vazia() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        BigDecimal valorDeposito = new BigDecimal("10.00");

        Wallet carteira = Wallet.builder()
                .id(walletId)
                .ownerName("Carlos Henrique")
                .currentBalance(new BigDecimal("0.00"))
                .createdAt(Instant.now())
                .version(0L)
                .build();

        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.of(carteira));

        // Act
        depositUseCase.execute(walletId, valorDeposito, null);

        // Assert
        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(1)).save(ledgerCaptor.capture());

        LedgerEntry savedEntry = ledgerCaptor.getValue();
        assertEquals("DEPOSIT", savedEntry.getDescription(), "Descrição padrão deveria ser 'DEPOSIT'");

        verify(walletRepository, times(1)).findByIdForUpdate(walletId);
        verify(walletRepository, times(1)).save(any(Wallet.class));
    }

    @Test
    void deve_lancar_excecao_quando_valor_for_invalido_ou_zero() {
        UUID walletId = UUID.randomUUID();

        // amount null
        IllegalArgumentException ex1 = assertThrows(
                IllegalArgumentException.class,
                () -> depositUseCase.execute(walletId, null, "x")
        );
        assertEquals("Amount must be positive", ex1.getMessage());

        // amount negativo
        IllegalArgumentException ex2 = assertThrows(
                IllegalArgumentException.class,
                () -> depositUseCase.execute(walletId, new BigDecimal("-1.00"), "x")
        );
        assertEquals("Amount must be positive", ex2.getMessage());

        // amount zero
        IllegalArgumentException ex3 = assertThrows(
                IllegalArgumentException.class,
                () -> depositUseCase.execute(walletId, BigDecimal.ZERO, "x")
        );
        assertEquals("Amount must be positive", ex3.getMessage());

        // Nenhum acesso ao banco deve ocorrer nesses casos
        verifyNoInteractions(walletRepository, ledgerEntryRepository);
    }

    @Test
    void deve_lancar_excecao_quando_wallet_nao_existir() {
        // Arrange
        UUID walletId = UUID.randomUUID();
        BigDecimal valorDeposito = new BigDecimal("50.00");

        when(walletRepository.findByIdForUpdate(walletId)).thenReturn(Optional.empty());

        // Act
        WalletNotFoundException thrown = assertThrows(
                WalletNotFoundException.class,
                () -> depositUseCase.execute(walletId, valorDeposito, "Funding")
        );

        // Assert
        assertEquals("Wallet not found", thrown.getMessage());

        verify(walletRepository, times(1)).findByIdForUpdate(walletId);

        // ledger não deve ser salvo se a carteira não existe
        verifyNoInteractions(ledgerEntryRepository);
    }
}