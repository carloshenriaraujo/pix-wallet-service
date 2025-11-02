package br.com.wallet.finance.domain.model;

import br.com.wallet.finance.domain.enums.LedgerEntryType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_wallet_time", columnList = "wallet_id, occurred_at"),
        @Index(name = "idx_ledger_endtoend", columnList = "end_to_end_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue
    @Column(name = "ledger_entry_id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private LedgerEntryType type; // DEBIT ou CREDIT

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "end_to_end_id", length = 64)
    private String endToEndId;

    @Column(name = "description", length = 120)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
