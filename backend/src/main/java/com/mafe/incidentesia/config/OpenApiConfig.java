package com.mafe.incidentesia.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API - Sistema Inteligente de Analisis de Incidentes y Asistencia IA")
                        .version("1.0.0")
                        .description("Contratos REST para la gestion de incidentes tecnologicos y orquestacion de asistencia mediante IA con n8n.")
                        .contact(new Contact()
                                .name("Mafe - Rama de Desarrollo")
                                .email("mafe@proyecto-incidentes.dev")));
    }
}

