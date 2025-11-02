package br.com.wallet.finance.api;

import br.com.wallet.finance.api.dto.request.DepositRequest;
import br.com.wallet.finance.api.dto.request.WithdrawRequest;
import br.com.wallet.finance.application.usecase.DepositUseCase;
import br.com.wallet.finance.application.usecase.GetBalanceUseCase;
import br.com.wallet.finance.application.usecase.GetHistoricalBalanceUseCase;
import br.com.wallet.finance.application.usecase.WithdrawUseCase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = WalletBalanceController.class)
class WalletBalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // mocks dos casos de uso que o controller injeta
    @MockBean
    private GetBalanceUseCase getBalanceUseCase;

    @MockBean
    private GetHistoricalBalanceUseCase getHistoricalBalanceUseCase;

    @MockBean
    private DepositUseCase depositUseCase;

    @MockBean
    private WithdrawUseCase withdrawUseCase;

    // mocks para satisfazer beans globais que a aplicação principal cria
    @MockBean
    private RestTemplateBuilder restTemplateBuilder;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setupRestTemplateBuilder() {
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance sem parâmetro 'at' deve retornar saldo atual")
    void shouldReturnCurrentBalance() throws Exception {
        UUID walletId = UUID.randomUUID();
        BigDecimal balance = new BigDecimal("123.45");

        Mockito.when(getBalanceUseCase.execute(walletId))
                .thenReturn(balance);

        mockMvc.perform(
                        get("/wallets/{walletId}/balance", walletId.toString())
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                // BalanceResponse(walletId, balance)
                .andExpect(jsonPath("$.walletId", is(walletId.toString())))
                .andExpect(jsonPath("$.balance", is(123.45)));

        Mockito.verify(getBalanceUseCase).execute(walletId);
        Mockito.verifyNoInteractions(getHistoricalBalanceUseCase);
    }

    @Test
    @DisplayName("GET /wallets/{id}/balance com parâmetro 'at' deve retornar saldo histórico naquele instante")
    void shouldReturnHistoricalBalance() throws Exception {
        UUID walletId = UUID.randomUUID();
        BigDecimal historicalBalance = new BigDecimal("88.00");

        // usamos um timestamp determinístico
        Instant atInstant = Instant.parse("2025-10-09T15:00:00Z");

        Mockito.when(getHistoricalBalanceUseCase.execute(walletId, atInstant))
                .thenReturn(historicalBalance);

        mockMvc.perform(
                        get("/wallets/{walletId}/balance", walletId.toString())
                                .param("at", atInstant.toString())
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                // BalanceResponse(walletId, balance)
                .andExpect(jsonPath("$.walletId", is(walletId.toString())))
                .andExpect(jsonPath("$.balance", is(88.00)));

        Mockito.verify(getHistoricalBalanceUseCase).execute(walletId, atInstant);
        Mockito.verifyNoInteractions(getBalanceUseCase);
    }

    @Test
    @DisplayName("POST /wallets/{id}/deposit deve chamar o caso de uso de depósito e retornar 201")
    void shouldDeposit() throws Exception {
        UUID walletId = UUID.randomUUID();

        DepositRequest requestBody = new DepositRequest(
                new BigDecimal("250.00"),
                "Initial funding"
        );

        // não precisamos stubbar depositUseCase porque ele retorna void,
        // então só vamos verificar depois se ele foi chamado

        mockMvc.perform(
                        post("/wallets/{walletId}/deposit", walletId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody))
                )
                .andExpect(status().isCreated());

        Mockito.verify(depositUseCase)
                .execute(walletId, new BigDecimal("250.00"), "Initial funding");
    }

    @Test
    @DisplayName("POST /wallets/{id}/withdraw deve chamar o caso de uso de saque e retornar 201")
    void shouldWithdraw() throws Exception {
        UUID walletId = UUID.randomUUID();

        WithdrawRequest requestBody = new WithdrawRequest(
                new BigDecimal("50.00"),
                "ATM cash out"
        );

        mockMvc.perform(
                        post("/wallets/{walletId}/withdraw", walletId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody))
                )
                .andExpect(status().isCreated());

        Mockito.verify(withdrawUseCase)
                .execute(walletId, new BigDecimal("50.00"), "ATM cash out");
    }
}