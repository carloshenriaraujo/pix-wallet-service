package br.com.wallet.finance.application.usecase.impl;

import br.com.wallet.finance.domain.exception.PixKeyAlreadyExistsException;
import br.com.wallet.finance.domain.exception.WalletNotFoundException;
import br.com.wallet.finance.domain.model.PixKey;
import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.infrastructure.repository.PixKeyRepository;
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

class RegisterPixKeyUseCaseImplTest {

    private WalletRepository walletRepository;
    private PixKeyRepository pixKeyRepository;
    private RegisterPixKeyUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        pixKeyRepository = mock(PixKeyRepository.class);
        useCase = new RegisterPixKeyUseCaseImpl(walletRepository, pixKeyRepository);
    }

    @Test
    void deveRegistrarChavePixComSucesso() {
        // given
        UUID walletId = UUID.randomUUID();
        String keyType = "EMAIL";
        String keyValue = "carlos@meva.com";

        Wallet wallet = Wallet.builder()
                .id(walletId)
                .ownerName("Carlos")
                .currentBalance(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .version(0L)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(pixKeyRepository.existsByKeyValue(keyValue)).thenReturn(false);

        // capturar o PixKey salvo
        ArgumentCaptor<PixKey> pixKeyCaptor = ArgumentCaptor.forClass(PixKey.class);
        when(pixKeyRepository.save(pixKeyCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        PixKey result = useCase.execute(walletId, keyType, keyValue);

        // then
        assertNotNull(result, "O retorno não deveria ser nulo");
        assertEquals(walletId, result.getWallet().getId(), "Wallet associada incorreta");
        assertEquals(keyType, result.getKeyType(), "Tipo de chave incorreto");
        assertEquals(keyValue, result.getKeyValue(), "Valor da chave incorreto");

        assertNotNull(result.getCreatedAt(), "CreatedAt deveria ser preenchido");

        // garante que chamou os repositórios corretos
        verify(walletRepository, times(1)).findById(walletId);
        verify(pixKeyRepository, times(1)).existsByKeyValue(keyValue);
        verify(pixKeyRepository, times(1)).save(any(PixKey.class));

        // valida o objeto realmente passado pro save
        PixKey salvo = pixKeyCaptor.getValue();
        assertEquals(keyType, salvo.getKeyType());
        assertEquals(keyValue, salvo.getKeyValue());
        assertEquals(wallet, salvo.getWallet());
        assertNotNull(salvo.getCreatedAt());
    }

    @Test
    void deveLancarExcecaoQuandoWalletNaoExiste() {
        // given
        UUID walletId = UUID.randomUUID();
        String keyType = "EMAIL";
        String keyValue = "carlos@meva.com";

        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        // when + then
        WalletNotFoundException ex = assertThrows(
                WalletNotFoundException.class,
                () -> useCase.execute(walletId, keyType, keyValue),
                "Deveria lançar WalletNotFoundException se a wallet não existe"
        );

        assertEquals("Wallet not found", ex.getMessage());

        verify(walletRepository, times(1)).findById(walletId);
        verifyNoMoreInteractions(walletRepository);
        verifyNoInteractions(pixKeyRepository); // nem tenta checar chave
    }

    @Test
    void deveLancarExcecaoQuandoChavePixJaExiste() {
        // given
        UUID walletId = UUID.randomUUID();
        String keyType = "EMAIL";
        String keyValue = "carlos@meva.com";

        Wallet wallet = Wallet.builder()
                .id(walletId)
                .ownerName("Carlos")
                .currentBalance(BigDecimal.ZERO)
                .createdAt(Instant.now())
                .version(0L)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(pixKeyRepository.existsByKeyValue(keyValue)).thenReturn(true);

        // when + then
        PixKeyAlreadyExistsException ex = assertThrows(
                PixKeyAlreadyExistsException.class,
                () -> useCase.execute(walletId, keyType, keyValue),
                "Deveria lançar IllegalStateException se a chave já estiver cadastrada"
        );

        assertEquals("Pix key already registered", ex.getMessage());

        verify(walletRepository, times(1)).findById(walletId);
        verify(pixKeyRepository, times(1)).existsByKeyValue(keyValue);
        verify(pixKeyRepository, never()).save(any());
    }
}