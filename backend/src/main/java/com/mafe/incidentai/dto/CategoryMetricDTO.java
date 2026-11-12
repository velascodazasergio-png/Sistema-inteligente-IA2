package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Contrato DTO para exponer la frecuencia y porcentaje de incidentes por categoria tecnica (RF06)
@Schema(description = "Metrica de frecuencia de incidentes agrupados por categoria tecnica")
public record CategoryMetricDTO(
        @Schema(description = "ID de la categoria", example = "1")
        Long categoryId,

        @Schema(description = "Nombre de la categoria tecnica", example = "Networking & Connectivity")
        String categoryName,

        @Schema(description = "Cantidad total de incidentes registrados en esta categoria", example = "48")
        Long incidentCount,

        @Schema(description = "Porcentaje que representa esta categoria sobre el total de incidentes", example = "32.0")
        Double percentageOfTotal
) {}

