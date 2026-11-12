package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.IncidentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// Contrato DTO para solicitar el cambio de estado de un incidente
@Schema(description = "Solicitud para actualizar el estado de un incidente existente")
public record UpdateIncidentStatusRequestDTO(

        @Schema(description = "Nuevo estado del incidente", example = "IN_AI_ANALYSIS")
        @NotNull(message = "New status is required")
        IncidentStatus status
) {}

