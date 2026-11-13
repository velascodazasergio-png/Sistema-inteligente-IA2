package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Reporte de fallo que envia n8n cuando no puede procesar una consulta (embeddings, Qdrant o LLM)
@Schema(description = "Fallo del flujo de IA reportado por n8n")
public record AiFailureReportDTO(
        @Schema(description = "ID de la consulta (debe coincidir con la ruta, opcional)", example = "6")
        Long aiQueryId,

        @Schema(description = "Codigo del fallo", example = "EMBEDDING_FALLO")
        @NotBlank(message = "Error code is required")
        @Size(max = 60, message = "Error code cannot exceed 60 characters")
        String errorCode,

        @Schema(description = "Mensaje legible del fallo", example = "No se pudo generar el embedding de la consulta.")
        @NotBlank(message = "Message is required")
        @Size(max = 500, message = "Message cannot exceed 500 characters")
        String message,

        @Schema(description = "Detalle tecnico (no se muestra al usuario final)", example = "ETIMEDOUT")
        @Size(max = 500, message = "Technical detail cannot exceed 500 characters")
        String technicalDetail,

        @Schema(description = "Tiempo transcurrido en n8n antes del fallo", example = "1200")
        Long processingTimeMs
) {}
