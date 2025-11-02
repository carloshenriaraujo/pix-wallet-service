package br.com.wallet.finance.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "webhook_events",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_webhook_event_id",
                        columnNames = {"event_id"}
                )
        },
        indexes = {
                @Index(name = "idx_webhook_endtoend", columnList = "end_to_end_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {

    @Id
    @GeneratedValue
    @Column(name = "webhook_event_pk", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "end_to_end_id", nullable = false, length = 64)
    private String endToEndId;

    @Column(name = "event_type", nullable = false, length = 20)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
