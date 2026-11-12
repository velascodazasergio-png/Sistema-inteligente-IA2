package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.IncidentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

// Contrato DTO para exponer la distribucion de incidentes agrupados por estado operativo (RF06)
@Schema(description = "Metrica de incidentes agrupados por estado")
public record StatusMetricDTO(
        @Schema(description = "Estado del incidente", example = "RESOLVED")
        IncidentStatus status,

        @Schema(description = "Cantidad de incidentes en este estado", example = "63")
        Long incidentCount,

        @Schema(description = "Porcentaje sobre el total de incidentes filtrados", example = "42.0")
        Double percentageOfTotal
) {}

