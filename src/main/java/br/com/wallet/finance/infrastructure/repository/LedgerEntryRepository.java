package br.com.wallet.finance.infrastructure.repository;

import br.com.wallet.finance.domain.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByWallet_IdAndOccurredAtLessThanEqual(UUID walletId, Instant occurredAt);
}
