package br.com.wallet.finance.application.usecase.impl;

import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.infrastructure.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetBalanceUseCaseImplTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private GetBalanceUseCaseImpl getBalanceUseCase;

    private UUID walletId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        walletId = UUID.randomUUID();
    }

    @Test
    void shouldReturnCurrentBalance_whenWalletExists() {
        // Arrange
        Wallet wallet = Wallet.builder()
                .id(walletId)
                .ownerName("Carlos Henrique")
                .currentBalance(new BigDecimal("1500.00"))
                .createdAt(Instant.now())
                .version(0L)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        // Act
        BigDecimal balance = getBalanceUseCase.execute(walletId);

        // Assert
        assertEquals(new BigDecimal("1500.00"), balance);
        verify(walletRepository, times(1)).findById(walletId);
    }

    @Test
    void shouldThrowException_whenWalletNotFound() {
        // Arrange
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> getBalanceUseCase.execute(walletId)
        );

        assertEquals("Wallet not found", ex.getMessage());
        verify(walletRepository, times(1)).findById(walletId);
    }
}