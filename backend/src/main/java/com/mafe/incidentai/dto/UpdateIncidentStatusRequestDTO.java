package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.IncidentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// DTO to request a status change for an incident
@Schema(description = "Request to update the status of an existing incident")
public record UpdateIncidentStatusRequestDTO(

        @Schema(description = "New status of the incident", example = "IN_AI_ANALYSIS")
        @NotNull(message = "New status is required")
        IncidentStatus status
) {}

