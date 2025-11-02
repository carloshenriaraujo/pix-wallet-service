package br.com.wallet.finance.api;

import br.com.wallet.finance.api.dto.PixWebhookApi;
import br.com.wallet.finance.api.dto.request.PixWebhookRequest;
import br.com.wallet.finance.application.usecase.ProcessWebhookUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PixWebhookController implements PixWebhookApi {

    private final ProcessWebhookUseCase processWebhookUseCase;

    public PixWebhookController(ProcessWebhookUseCase processWebhookUseCase) {
        this.processWebhookUseCase = processWebhookUseCase;
    }

    @Override
    public void handleWebhook(@Valid PixWebhookRequest request) {
        processWebhookUseCase.execute(
                request.eventId(),
                request.endToEndId(),
                request.eventType(),
                request.occurredAt()
        );
    }
}
