package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

// Optional filters shared by the analytics endpoints (query params). A null field does not filter.
@Schema(description = "Optional filters for the analytics indicators")
public record AnalyticsFilterRequest(
        @Schema(description = "ID of the technical category", example = "1")
        Long categoryId,

        @Schema(description = "Priority level", example = "HIGH")
        Priority priority,

        @Schema(description = "Start date (ISO 8601)", example = "2026-10-01T00:00:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startDate,

        @Schema(description = "End date (ISO 8601)", example = "2026-11-30T23:59:59")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endDate
) {}
