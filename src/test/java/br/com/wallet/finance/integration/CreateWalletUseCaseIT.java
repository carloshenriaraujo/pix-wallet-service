package br.com.wallet.finance.integration;

import br.com.wallet.finance.application.usecase.CreateWalletUseCase;
import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.infrastructure.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração:
 * - Sobe o contexto Spring completo
 * - Usa o banco H2 real (profile "test")
 * - Valida se a carteira foi criada com saldo zero e persistida
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CreateWalletUseCaseIT {

    @Autowired
    private CreateWalletUseCase createWalletUseCase;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void shouldCreateWalletWithZeroInitialBalanceAndPersist() {

        String ownerName = "Carlos Henrique";
        Wallet created = createWalletUseCase.execute(ownerName);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getOwnerName()).isEqualTo(ownerName);
        assertThat(created.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getVersion()).isNotNull();

        Wallet fromDb = walletRepository.findById(created.getId()).orElseThrow();
        assertThat(fromDb.getOwnerName()).isEqualTo(ownerName);
        assertThat(fromDb.getCurrentBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(fromDb.getCreatedAt()).isNotNull();
        assertThat(fromDb.getVersion()).isNotNull();
    }
}

