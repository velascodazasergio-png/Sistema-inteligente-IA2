package com.mafe.incidentai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Global Swagger / OpenAPI 3 configuration that documents the REST contracts of the system
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    // API title, description and Bearer (JWT) security scheme shown in Swagger
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // Swagger "Authorize" button: paste the accessToken from POST /api/v1/auth/login
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .info(new Info()
                        .title("Intelligent Incident Analysis & AI Assistance API")
                        .version("1.0.0")
                        .description("REST contracts for managing technology incidents and orchestrating AI assistance with n8n.")
                        .contact(new Contact()
                                .name("Mafe - Development Branch")
                                .email("mafe@incident-ai.dev")));
    }
}

