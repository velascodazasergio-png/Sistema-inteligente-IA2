package com.mafe.incidentai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuracion global de Swagger / OpenAPI 3 para documentar los contratos REST del sistema
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // Boton "Authorize" de Swagger: pegar el accessToken de POST /api/v1/auth/login
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .info(new Info()
                        .title("Intelligent Incident Analysis & AI Assistance API")
                        .version("1.0.0")
                        .description("Contratos REST para la gestion de incidentes tecnologicos y orquestacion de asistencia mediante IA con n8n.")
                        .contact(new Contact()
                                .name("Mafe - Development Branch")
                                .email("mafe@incident-ai.dev")));
    }
}

