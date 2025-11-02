package br.com.wallet.finance.domain.model;

import br.com.wallet.finance.domain.enums.PixTransferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "pix_transfers",
        uniqueConstraints = {
                // garante idempotência por carteira de origem + idempotencyKey
                @UniqueConstraint(
                        name = "uk_transfer_fromwallet_idem",
                        columnNames = {"from_wallet_id", "idempotency_key"}
                ),
                // garante unicidade global de endToEndId
                @UniqueConstraint(
                        name = "uk_transfer_endtoend",
                        columnNames = {"end_to_end_id"}
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixTransfer {

    @Id
    @GeneratedValue
    @Column(name = "pix_transfer_id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "from_wallet_id", nullable = false)
    private Wallet fromWallet;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    @Column(name = "to_pix_key", nullable = false, length = 140)
    private String toPixKey; // redundância de auditoria

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "end_to_end_id", nullable = false, length = 64)
    private String endToEndId;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PixTransferStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
