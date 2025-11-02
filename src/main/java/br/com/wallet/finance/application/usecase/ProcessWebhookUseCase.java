package br.com.wallet.finance.application.usecase;

import java.time.Instant;

public interface ProcessWebhookUseCase {

    void execute(
            String eventId,
            String endToEndId,
            String eventType,
            Instant occurredAt
    );
}