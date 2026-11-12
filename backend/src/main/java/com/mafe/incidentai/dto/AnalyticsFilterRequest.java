package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

// Filtros opcionales compartidos por los endpoints de analitica (query params). Un campo nulo no filtra.
@Schema(description = "Filtros opcionales de los indicadores analiticos")
public record AnalyticsFilterRequest(
        @Schema(description = "ID de la categoria tecnica", example = "1")
        Long categoryId,

        @Schema(description = "Nivel de prioridad", example = "HIGH")
        Priority priority,

        @Schema(description = "Fecha inicial (ISO 8601)", example = "2026-10-01T00:00:00")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime startDate,

        @Schema(description = "Fecha final (ISO 8601)", example = "2026-11-30T23:59:59")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime endDate
) {}

