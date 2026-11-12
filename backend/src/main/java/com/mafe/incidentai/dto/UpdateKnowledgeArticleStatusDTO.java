package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Cambio parcial de los indicadores de un articulo. Los campos nulos no se modifican.
@Schema(description = "Activa/desactiva o verifica un articulo. Los campos omitidos no cambian.")
public record UpdateKnowledgeArticleStatusDTO(
        @Schema(description = "true para activar, false para desactivar", example = "true")
        Boolean active,

        @Schema(description = "true para marcar como verificado", example = "true")
        Boolean verified
) {}

