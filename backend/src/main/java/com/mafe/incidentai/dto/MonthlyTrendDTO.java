package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Incidentes reportados y resueltos en un mes
@Schema(description = "Tendencia mensual de incidentes reportados vs resueltos")
public record MonthlyTrendDTO(
        @Schema(description = "Mes en formato AAAA-MM", example = "2026-10")
        String yearMonth,

        @Schema(description = "Incidentes registrados en el mes", example = "80")
        Long totalReported,

        @Schema(description = "De esos incidentes, cuantos estan resueltos o cerrados", example = "72")
        Long totalResolved
) {}
