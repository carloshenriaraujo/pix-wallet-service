package br.com.wallet.finance.api;

import br.com.wallet.finance.api.dto.request.PixTransferRequest;
import br.com.wallet.finance.application.usecase.CreatePixTransferUseCase;
import br.com.wallet.finance.domain.enums.PixTransferStatus;
import br.com.wallet.finance.domain.model.PixTransfer;
import br.com.wallet.finance.domain.model.Wallet;
import br.com.wallet.finance.api.error.GlobalExceptionHandler;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = { PixTransferController.class, GlobalExceptionHandler.class })
class PixTransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // mock do caso de uso que o controller injeta
    @MockBean
    private CreatePixTransferUseCase createPixTransferUseCase;

    // mocks para satisfazer beans globais como RestTemplate da @SpringBootApplication
    @MockBean
    private RestTemplateBuilder restTemplateBuilder;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setupRestTemplateBuilder() {
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
    }

    @Test
    @DisplayName("POST /pix/transfers deve criar transferência Pix PENDING e retornar 201 com endToEndId e status")
    void shouldCreatePixTransfer() throws Exception {
        UUID fromWalletId = UUID.randomUUID();
        String toPixKey = "carlos@meva.com";
        BigDecimal amount = new BigDecimal("150.00");
        String idempotencyKey = "123e4567-e89b-12d3-a456-426614174000";
        String endToEndId = UUID.randomUUID().toString();

        Wallet fromWallet = Wallet.builder()
                .id(fromWalletId)
                .ownerName("Carlos Henrique")
                .currentBalance(new BigDecimal("500.00"))
                .createdAt(Instant.parse("2025-10-09T12:00:00Z"))
                .version(0L)
                .build();

        Wallet toWallet = Wallet.builder()
                .id(UUID.randomUUID())
                .ownerName("Destinatário")
                .currentBalance(new BigDecimal("40.00"))
                .createdAt(Instant.parse("2025-10-09T11:00:00Z"))
                .version(0L)
                .build();

        PixTransfer transferMock = PixTransfer.builder()
                .id(UUID.randomUUID())
                .fromWallet(fromWallet)
                .toWallet(toWallet)
                .toPixKey(toPixKey)
                .amount(amount)
                .endToEndId(endToEndId)
                .idempotencyKey(idempotencyKey)
                .status(PixTransferStatus.PENDING)
                .createdAt(Instant.parse("2025-10-09T15:05:00Z"))
                .updatedAt(Instant.parse("2025-10-09T15:05:00Z"))
                .version(0L)
                .build();

        Mockito.when(
                createPixTransferUseCase.execute(
                        idempotencyKey,
                        fromWalletId,
                        toPixKey,
                        amount
                )
        ).thenReturn(transferMock);

        PixTransferRequest requestBody = new PixTransferRequest(
                fromWalletId,
                toPixKey,
                amount
        );

        mockMvc.perform(
                        post("/pix/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Idempotency-Key", idempotencyKey)
                                .content(objectMapper.writeValueAsString(requestBody))
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.endToEndId", is(endToEndId)))
                .andExpect(jsonPath("$.status", is("PENDING")));

        Mockito.verify(createPixTransferUseCase)
                .execute(idempotencyKey, fromWalletId, toPixKey, amount);
    }

    @Test
    @DisplayName("POST /pix/transfers sem Idempotency-Key deve retornar 400 com ErrorResponse JSON")
    void shouldReturnBadRequestWhenMissingIdempotencyKey() throws Exception {
        UUID fromWalletId = UUID.randomUUID();
        String toPixKey = "carlos@meva.com";
        BigDecimal amount = new BigDecimal("150.00");

        PixTransferRequest requestBody = new PixTransferRequest(
                fromWalletId,
                toPixKey,
                amount
        );

        mockMvc.perform(
                        post("/pix/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                // NÃO mando o header Idempotency-Key de propósito
                                .content(objectMapper.writeValueAsString(requestBody))
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                // valida JSON padrão do GlobalExceptionHandler
                .andExpect(jsonPath("$.error", is("MISSING_HEADER")))
                .andExpect(jsonPath("$.message", containsString("Idempotency-Key")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}