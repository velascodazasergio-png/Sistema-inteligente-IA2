package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// DTO with the weekly trend of incidents (RF06)
@Schema(description = "Weekly trend of reported and resolved incidents")
public record WeeklyTrendDTO(
        @Schema(description = "ISO week identifier (Year-Week)", example = "2026-W44")
        String yearWeek,

        @Schema(description = "Total incidents reported in that week", example = "28")
        Long totalReported,

        @Schema(description = "Total incidents resolved or closed in that week", example = "24")
        Long totalResolved
) {}

