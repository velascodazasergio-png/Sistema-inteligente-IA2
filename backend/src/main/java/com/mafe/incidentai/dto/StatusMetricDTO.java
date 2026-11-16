package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.IncidentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

// DTO with the distribution of incidents grouped by operational status (RF06)
@Schema(description = "Incidents grouped by status")
public record StatusMetricDTO(
        @Schema(description = "Incident status", example = "RESOLVED")
        IncidentStatus status,

        @Schema(description = "Number of incidents in this status", example = "63")
        Long incidentCount,

        @Schema(description = "Percentage of the filtered incidents", example = "42.0")
        Double percentageOfTotal
) {}

