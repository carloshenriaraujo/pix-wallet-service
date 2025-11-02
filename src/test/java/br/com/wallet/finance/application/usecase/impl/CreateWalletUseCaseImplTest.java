package br.com.wallet.finance.application.usecase.impl;

import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.infrastructure.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreateWalletUseCaseImplTest {

    private WalletRepository walletRepository;
    private CreateWalletUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        useCase = new CreateWalletUseCaseImpl(walletRepository);
    }

    @Test
    void shouldCreateWalletWithInitialBalanceZero() {
        // arrange
        String ownerName = "Carlos Henrique";

        // mock: simula o retorno do save
        Wallet savedWallet = Wallet.builder()
                .id(java.util.UUID.randomUUID())
                .ownerName(ownerName)
                .currentBalance(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .version(0L)
                .build();

        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        // act
        Wallet result = useCase.execute(ownerName);

        // assert
        assertNotNull(result);
        assertEquals(ownerName, result.getOwnerName());
        assertEquals(BigDecimal.ZERO, result.getCurrentBalance());
        assertEquals(0L, result.getVersion());

        // captura o argumento passado ao repositório
        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository, times(1)).save(captor.capture());

        Wallet walletToSave = captor.getValue();
        assertEquals(ownerName, walletToSave.getOwnerName());
        assertEquals(BigDecimal.ZERO, walletToSave.getCurrentBalance());
        assertNotNull(walletToSave.getCreatedAt());
    }

    @Test
    void shouldThrowExceptionWhenOwnerNameIsNull() {
        assertThrows(IllegalArgumentException.class, () -> useCase.execute(null));
    }
}
