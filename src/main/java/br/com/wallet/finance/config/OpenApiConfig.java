package br.com.wallet.finance.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pixWalletOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Pix Wallet Service API")
                                .description("Microserviço de Carteira Digital com suporte a Pix.\n" +
                                        "Inclui criação de carteira, chaves Pix, saldo histórico, depósito, saque, " +
                                        "transferência Pix e processamento de webhook com idempotência.")
                                .version("0.0.1")
                                .contact(new Contact()
                                        .name("Pix Wallet Team")
                                        .email("engineering@finaya.com"))
                                .license(new License()
                                        .name("Proprietary / Assessment Use Only")
                                )
                )
                .externalDocs(
                        new ExternalDocumentation()
                                .description("Desafio técnico - Pix Service")
                );
    }
}




