package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;

// DTO with the distribution of incidents grouped by priority level (RF06)
@Schema(description = "Incidents grouped by priority level")
public record PriorityMetricDTO(
        @Schema(description = "Priority level", example = "CRITICAL")
        Priority priority,

        @Schema(description = "Number of incidents with this priority", example = "25")
        Long incidentCount,

        @Schema(description = "Percentage of the filtered incidents", example = "16.67")
        Double percentageOfTotal
) {}

