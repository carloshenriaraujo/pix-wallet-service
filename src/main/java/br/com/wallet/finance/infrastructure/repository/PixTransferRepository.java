package br.com.wallet.finance.infrastructure.repository;

import br.com.wallet.finance.domain.model.PixTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PixTransferRepository extends JpaRepository<PixTransfer, UUID> {

    Optional<PixTransfer> findByEndToEndId(String endToEndId);

    Optional<PixTransfer> findByFromWallet_IdAndIdempotencyKey(UUID fromWalletId, String idempotencyKey);
}
