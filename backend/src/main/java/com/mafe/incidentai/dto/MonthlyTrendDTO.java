package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Contrato DTO para representar la evolucion mensual de incidentes reportados vs resueltos (RF06)
@Schema(description = "Tendencia mensual de incidentes reportados y resueltos")
public record MonthlyTrendDTO(
        @Schema(description = "Identificador del mes en formato AAAA-MM", example = "2026-10")
        String yearMonth,

        @Schema(description = "Total de incidentes reportados en ese mes", example = "82")
        Long totalReported,

        @Schema(description = "Total de incidentes resueltos o cerrados en ese mes", example = "71")
        Long totalResolved
) {}

