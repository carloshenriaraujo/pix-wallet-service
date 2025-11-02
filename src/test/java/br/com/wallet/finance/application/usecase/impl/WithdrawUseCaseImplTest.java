package br.com.wallet.finance.application.usecase.impl;

import br.com.wallet.finance.domain.enums.LedgerEntryType;
import br.com.wallet.finance.domain.exception.InsufficientFundsException;
import br.com.wallet.finance.domain.exception.WalletNotFoundException;
import br.com.wallet.finance.domain.model.LedgerEntry;
import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.infrastructure.repository.LedgerEntryRepository;
import br.com.wallet.finance.infrastructure.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WithdrawUseCaseImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private WithdrawUseCaseImpl withdrawUseCase;

    private UUID walletId;
    private Wallet wallet;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        walletId = UUID.randomUUID();
        wallet = Wallet.builder()
                .id(walletId)
                .ownerName("Carlos Henrique")
                .currentBalance(new BigDecimal("500.00"))
                .createdAt(Instant.now())
                .version(0L)
                .build();
    }

    @Test
    void deve_realizar_saque_com_sucesso() {
        // given
        BigDecimal amount = new BigDecimal("100.00");
        String description = "ATM WITHDRAW";

        // mock: ao travar a wallet (findByIdForUpdate), retornar a carteira existente
        when(walletRepository.findByIdForUpdate(walletId))
                .thenReturn(Optional.of(wallet));

        // mock: salvar ledger ok
        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // mock: salvar wallet atualizada ok
        when(walletRepository.save(any(Wallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        assertDoesNotThrow(() ->
                withdrawUseCase.execute(walletId, amount, description)
        );

        // then
        // valida que criou lançamento de débito
        ArgumentCaptor<LedgerEntry> ledgerCaptor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryRepository, times(1)).save(ledgerCaptor.capture());

        LedgerEntry savedEntry = ledgerCaptor.getValue();
        assertEquals(wallet, savedEntry.getWallet());
        assertEquals(LedgerEntryType.DEBIT, savedEntry.getType());
        assertEquals(new BigDecimal("100.00"), savedEntry.getAmount());
        assertEquals("ATM WITHDRAW", savedEntry.getDescription());
        assertNotNull(savedEntry.getOccurredAt());

        // valida que o saldo da wallet foi atualizado
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, times(1)).save(walletCaptor.capture());

        Wallet updatedWallet = walletCaptor.getValue();
        assertEquals(new BigDecimal("400.00"), updatedWallet.getCurrentBalance());

        // valida ordem de chamada principal
        verify(walletRepository, times(1)).findByIdForUpdate(walletId);
        verifyNoMoreInteractions(walletRepository);
    }

    @Test
    void deve_lancar_erro_quando_amount_for_nulo_ou_negativo() {
        // amount nulo
        IllegalArgumentException ex1 = assertThrows(
                IllegalArgumentException.class,
                () -> withdrawUseCase.execute(walletId, null, "teste")
        );
        assertEquals("Amount must be positive", ex1.getMessage());

        // amount zero
        IllegalArgumentException ex2 = assertThrows(
                IllegalArgumentException.class,
                () -> withdrawUseCase.execute(walletId, BigDecimal.ZERO, "teste")
        );
        assertEquals("Amount must be positive", ex2.getMessage());

        // amount negativo
        IllegalArgumentException ex3 = assertThrows(
                IllegalArgumentException.class,
                () -> withdrawUseCase.execute(walletId, new BigDecimal("-5.00"), "teste")
        );
        assertEquals("Amount must be positive", ex3.getMessage());

        // nenhum acesso ao repo deve ocorrer nesse cenário
        verifyNoInteractions(walletRepository, ledgerEntryRepository);
    }

    @Test
    void deve_lancar_erro_quando_wallet_nao_existir() {
        // given
        BigDecimal amount = new BigDecimal("50.00");

        // mock: carteira não encontrada
        when(walletRepository.findByIdForUpdate(walletId))
                .thenReturn(Optional.empty());

        // when
        WalletNotFoundException ex = assertThrows(
                WalletNotFoundException.class,
                () -> withdrawUseCase.execute(walletId, amount, "WITHDRAW")
        );

        // then
        assertEquals("Wallet not found", ex.getMessage());
        verify(walletRepository, times(1)).findByIdForUpdate(walletId);
        verifyNoMoreInteractions(walletRepository);
        verifyNoInteractions(ledgerEntryRepository);
    }

    @Test
    void deve_lancar_erro_quando_nao_tiver_saldo_suficiente() {
        // given
        BigDecimal amount = new BigDecimal("9999.99");

        // mock: carteira existe mas com saldo menor que o valor do saque
        when(walletRepository.findByIdForUpdate(walletId))
                .thenReturn(Optional.of(wallet));

        // when
        InsufficientFundsException ex = assertThrows(
                InsufficientFundsException.class,
                () -> withdrawUseCase.execute(walletId, amount, "WITHDRAW")
        );

        // then
        assertEquals("Insufficient funds", ex.getMessage());

        // nessa falha, não deve registrar ledger nem salvar wallet
        verify(walletRepository, times(1)).findByIdForUpdate(walletId);
        verifyNoMoreInteractions(walletRepository);
        verifyNoInteractions(ledgerEntryRepository);
    }
}