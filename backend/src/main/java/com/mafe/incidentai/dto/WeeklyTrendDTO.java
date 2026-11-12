package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Contrato DTO para representar la evolucion y tendencia semanal de incidentes (RF06)
@Schema(description = "Tendencia semanal de incidentes reportados y resueltos")
public record WeeklyTrendDTO(
        @Schema(description = "Identificador de la semana ISO (Ano-Semana)", example = "2026-W44")
        String yearWeek,

        @Schema(description = "Total de incidentes reportados en esa semana", example = "28")
        Long totalReported,

        @Schema(description = "Total de incidentes resueltos o cerrados en esa semana", example = "24")
        Long totalResolved
) {}

