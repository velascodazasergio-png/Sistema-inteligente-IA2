package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;

// Contrato DTO para exponer la distribucion de incidentes agrupados por nivel de prioridad (RF06)
@Schema(description = "Metrica de incidentes agrupados por nivel de prioridad")
public record PriorityMetricDTO(
        @Schema(description = "Nivel de prioridad", example = "CRITICAL")
        Priority priority,

        @Schema(description = "Cantidad de incidentes con esta prioridad", example = "25")
        Long incidentCount,

        @Schema(description = "Porcentaje sobre el total de incidentes filtrados", example = "16.67")
        Double percentageOfTotal
) {}

