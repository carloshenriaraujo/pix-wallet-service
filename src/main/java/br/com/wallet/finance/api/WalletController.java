package br.com.wallet.finance.api;

import br.com.wallet.finance.api.dto.WalletApi;
import br.com.wallet.finance.api.dto.request.CreateWalletRequest;
import br.com.wallet.finance.api.dto.response.CreateWalletResponse;
import br.com.wallet.finance.api.dto.request.RegisterPixKeyRequest;
import br.com.wallet.finance.api.dto.response.RegisterPixKeyResponse;
import br.com.wallet.finance.application.usecase.CreateWalletUseCase;
import br.com.wallet.finance.application.usecase.RegisterPixKeyUseCase;
import br.com.wallet.finance.domain.model.PixKey;
import br.com.wallet.finance.domain.model.Wallet;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class WalletController implements WalletApi {

    private final CreateWalletUseCase createWalletUseCase;
    private final RegisterPixKeyUseCase registerPixKeyUseCase;

    public WalletController(CreateWalletUseCase createWalletUseCase,
                            RegisterPixKeyUseCase registerPixKeyUseCase) {
        this.createWalletUseCase = createWalletUseCase;
        this.registerPixKeyUseCase = registerPixKeyUseCase;
    }

    @Override
    public CreateWalletResponse createWallet(@Valid CreateWalletRequest req) {
        Wallet wallet = createWalletUseCase.execute(req.ownerName());
        return new CreateWalletResponse(
                wallet.getId(),
                wallet.getOwnerName(),
                wallet.getCurrentBalance(),
                wallet.getCreatedAt()
        );
    }

    @Override
    public RegisterPixKeyResponse registerPixKey(UUID walletId, @Valid RegisterPixKeyRequest req) {
        PixKey pixKey = registerPixKeyUseCase.execute(walletId, req.keyType(), req.keyValue());
        return new RegisterPixKeyResponse(
                pixKey.getId(),
                pixKey.getWallet().getId(),
                pixKey.getKeyType(),
                pixKey.getKeyValue(),
                pixKey.getCreatedAt()
        );
    }
}