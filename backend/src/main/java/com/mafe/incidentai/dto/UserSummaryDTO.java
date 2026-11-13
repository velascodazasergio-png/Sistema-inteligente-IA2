package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

// Datos publicos de un usuario para selectores (quien reporta, quien califica, autor)
@Schema(description = "Resumen de un usuario del sistema")
public record UserSummaryDTO(
        Long id,
        String fullName,
        String email,
        UserRole role
) {}
