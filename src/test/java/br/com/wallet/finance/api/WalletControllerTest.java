package br.com.wallet.finance.api;

import br.com.wallet.finance.api.dto.request.CreateWalletRequest;
import br.com.wallet.finance.api.dto.request.RegisterPixKeyRequest;
import br.com.wallet.finance.application.usecase.CreateWalletUseCase;
import br.com.wallet.finance.application.usecase.RegisterPixKeyUseCase;
import br.com.wallet.finance.domain.model.PixKey;
import br.com.wallet.finance.domain.model.Wallet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // mocks dos casos de uso que o controller injeta
    @MockBean
    private CreateWalletUseCase createWalletUseCase;

    @MockBean
    private RegisterPixKeyUseCase registerPixKeyUseCase;

    // ======= IMPORTANTE =======
    // Esses dois mocks abaixo são para satisfazer o contexto da sua @SpringBootApplication,
    // que cria um bean RestTemplate usando RestTemplateBuilder.
    @MockBean
    private RestTemplateBuilder restTemplateBuilder;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setupRestTemplateBuilder() {
        // sempre que a aplicação tentar chamar restTemplateBuilder.build(),
        // vamos devolver o mock de RestTemplate em vez de null.
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
    }

    @Test
    @DisplayName("POST /wallets deve criar uma carteira e retornar 201 com os dados")
    void shouldCreateWallet() throws Exception {
        UUID walletId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2025-10-09T15:00:00Z");

        Wallet walletMock = Wallet.builder()
                .id(walletId)
                .ownerName("Carlos Henrique")
                .currentBalance(BigDecimal.ZERO)
                .createdAt(createdAt)
                .version(0L)
                .build();

        Mockito.when(createWalletUseCase.execute("Carlos Henrique"))
                .thenReturn(walletMock);

        CreateWalletRequest requestBody = new CreateWalletRequest("Carlos Henrique");

        mockMvc.perform(
                        post("/wallets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody))
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(walletId.toString())))
                .andExpect(jsonPath("$.ownerName", is("Carlos Henrique")))
                .andExpect(jsonPath("$.currentBalance", is(0)))
                .andExpect(jsonPath("$.createdAt", is(createdAt.toString())));

        Mockito.verify(createWalletUseCase).execute("Carlos Henrique");
    }

    @Test
    @DisplayName("POST /wallets/{id}/pix-keys deve registrar chave Pix e retornar 201")
    void shouldRegisterPixKey() throws Exception {
        UUID walletId = UUID.randomUUID();
        UUID pixKeyId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2025-10-09T16:00:00Z");

        PixKey pixKeyMock = PixKey.builder()
                .id(pixKeyId)
                .wallet(
                        Wallet.builder()
                                .id(walletId)
                                .ownerName("Carlos Henrique")
                                .currentBalance(BigDecimal.valueOf(150.00))
                                .createdAt(Instant.parse("2025-10-09T12:00:00Z"))
                                .version(0L)
                                .build()
                )
                .keyType("EMAIL")
                .keyValue("carlos@meva.com")
                .createdAt(createdAt)
                .build();

        Mockito.when(
                registerPixKeyUseCase.execute(walletId, "EMAIL", "carlos@meva.com")
        ).thenReturn(pixKeyMock);

        RegisterPixKeyRequest body = new RegisterPixKeyRequest(
                "EMAIL",
                "carlos@meva.com"
        );

        mockMvc.perform(
                        post("/wallets/{walletId}/pix-keys", walletId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body))
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(pixKeyId.toString())))
                .andExpect(jsonPath("$.walletId", is(walletId.toString())))
                .andExpect(jsonPath("$.keyType", is("EMAIL")))
                .andExpect(jsonPath("$.keyValue", is("carlos@meva.com")))
                .andExpect(jsonPath("$.createdAt", is(createdAt.toString())));

        Mockito.verify(registerPixKeyUseCase)
                .execute(walletId, "EMAIL", "carlos@meva.com");
    }
}