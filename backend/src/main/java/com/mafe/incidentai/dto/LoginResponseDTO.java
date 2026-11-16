package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

// Session started: JWT for the Authorization header and the user data
@Schema(description = "Access token and authenticated user")
public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        UserSummaryDTO user
) {}
