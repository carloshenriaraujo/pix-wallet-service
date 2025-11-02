package br.com.wallet.finance.api;

import br.com.wallet.finance.api.dto.request.PixWebhookRequest;
import br.com.wallet.finance.application.usecase.ProcessWebhookUseCase;
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

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PixWebhookController.class)
class PixWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // mock do caso de uso injetado no controller
    @MockBean
    private ProcessWebhookUseCase processWebhookUseCase;

    // mocks necessários para satisfazer beans globais criados na Application
    @MockBean
    private RestTemplateBuilder restTemplateBuilder;

    @MockBean
    private RestTemplate restTemplate;

    @BeforeEach
    void setupRestTemplateBuilder() {
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
    }

    @Test
    @DisplayName("POST /pix/webhook deve aceitar o evento e retornar 202")
    void shouldAcceptWebhookEvent() throws Exception {
        // given
        String eventId = "evt-123";
        String endToEndId = "E2E-999";
        String eventType = "CONFIRMED";
        Instant occurredAt = Instant.parse("2025-10-09T15:30:00Z");

        PixWebhookRequest requestBody = new PixWebhookRequest(
                endToEndId,
                eventId,
                eventType,
                occurredAt
        );

        // when/then
        mockMvc.perform(
                        post("/pix/webhook")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestBody))
                )
                .andExpect(status().isAccepted());

        Mockito.verify(processWebhookUseCase).execute(
                endToEndId,
                eventId,
                eventType,
                occurredAt
        );
    }
}