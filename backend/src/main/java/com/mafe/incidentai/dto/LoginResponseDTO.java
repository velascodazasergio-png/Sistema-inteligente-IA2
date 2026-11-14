package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

// Sesion iniciada: token JWT para el encabezado Authorization y datos del usuario
@Schema(description = "Token de acceso y usuario autenticado")
public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UserSummaryDTO user
) {}
