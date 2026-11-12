package com.mafe.incidentai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuracion global de Swagger / OpenAPI 3 para documentar los contratos REST del sistema
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Intelligent Incident Analysis & AI Assistance API")
                        .version("1.0.0")
                        .description("Contratos REST para la gestion de incidentes tecnologicos y orquestacion de asistencia mediante IA con n8n.")
                        .contact(new Contact()
                                .name("Mafe - Development Branch")
                                .email("mafe@incident-ai.dev")));
    }
}

