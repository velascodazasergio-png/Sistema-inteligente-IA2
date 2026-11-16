package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// DTO with the number and percentage of incidents per technical category (RF06)
@Schema(description = "Incident frequency grouped by technical category")
public record CategoryMetricDTO(
        @Schema(description = "Category ID", example = "1")
        Long categoryId,

        @Schema(description = "Name of the technical category", example = "Networking & Connectivity")
        String categoryName,

        @Schema(description = "Total number of incidents registered in this category", example = "48")
        Long incidentCount,

        @Schema(description = "Percentage of all incidents that belong to this category", example = "32.0")
        Double percentageOfTotal
) {}

