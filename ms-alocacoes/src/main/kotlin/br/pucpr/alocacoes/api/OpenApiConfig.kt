package br.pucpr.alocacoes.api

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Metadados exibidos no Swagger UI (/swagger-ui.html). */
@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("Microsserviço de Alocações")
                .version("1.0.0")
                .description(
                    "Sistema de Controle de Ativos de Informática (PJBL/PUCPR). " +
                        "Colaboradores e alocações de ativos, persistidos no Azure SQL.",
                ),
        )
}
