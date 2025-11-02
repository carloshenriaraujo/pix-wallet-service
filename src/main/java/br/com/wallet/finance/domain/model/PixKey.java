package br.com.wallet.finance.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "pix_keys",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pix_key_value", columnNames = {"key_value"})
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixKey {

    @Id
    @GeneratedValue
    @Column(name = "pix_key_id", columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "key_type", nullable = false, length = 20)
    private String keyType;

    @Column(name = "key_value", nullable = false, length = 140)
    private String keyValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
