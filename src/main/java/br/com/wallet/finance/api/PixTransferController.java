package br.com.wallet.finance.api;

import br.com.wallet.finance.api.dto.PixTransferApi;
import br.com.wallet.finance.api.dto.request.PixTransferRequest;
import br.com.wallet.finance.api.dto.response.PixTransferResponse;
import br.com.wallet.finance.application.usecase.CreatePixTransferUseCase;
import br.com.wallet.finance.domain.model.PixTransfer;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PixTransferController implements PixTransferApi {

    private final CreatePixTransferUseCase createPixTransferUseCase;

    public PixTransferController(CreatePixTransferUseCase createPixTransferUseCase) {
        this.createPixTransferUseCase = createPixTransferUseCase;
    }

    @Override
    public PixTransferResponse createTransfer(
            String idempotencyKey,
            @Valid PixTransferRequest request
    ) {
        PixTransfer transfer = createPixTransferUseCase.execute(
                idempotencyKey,
                request.fromWalletId(),
                request.toPixKey(),
                request.amount()
        );

        return new PixTransferResponse(
                transfer.getEndToEndId(),
                transfer.getStatus().name()
        );
    }
}
