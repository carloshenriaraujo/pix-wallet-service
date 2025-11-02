package br.com.wallet.finance.infrastructure.repository;

import br.com.wallet.finance.domain.model.PixKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PixKeyRepository extends JpaRepository<PixKey, UUID> {

    Optional<PixKey> findByKeyValue(String keyValue);
    boolean existsByKeyValue(String keyValue);
}
