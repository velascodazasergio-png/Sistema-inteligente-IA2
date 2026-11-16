package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Incidents reported and resolved in a month
@Schema(description = "Monthly trend of reported vs resolved incidents")
public record MonthlyTrendDTO(
        @Schema(description = "Month in YYYY-MM format", example = "2026-10")
        String yearMonth,

        @Schema(description = "Incidents registered in the month", example = "80")
        Long totalReported,

        @Schema(description = "How many of those incidents are resolved or closed", example = "72")
        Long totalResolved
) {}
