package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// Contrato DTO que n8n envia de regreso a Spring Boot con el diagnostico de la IA y las fuentes consultadas
@Schema(description = "Contrato REST que n8n envia de vuelta con el diagnostico de la IA y sus fuentes documentales")
public record AiResponseDTO(

        @Schema(description = "ID de la consulta IA (AiQuery) original que disparo el analisis", example = "55")
        @NotNull(message = "AI Query ID is required to link the response")
        Long aiQueryId,

        @Schema(description = "Diagnostico probable generado por el agente de IA", example = "Possible UDP port 500/4500 (IPSec NAT-T) block on perimeter firewall")
        @NotBlank(message = "Suggested diagnosis cannot be blank")
        String suggestedDiagnosis,

        @Schema(description = "Pasos recomendados para resolver el incidente", example = "1. Verify UDP 500 and 4500 rules. 2. Restart IKEEXT service on client.")
        String solutionSteps,

        @Schema(description = "Nivel de confianza del modelo entre 0.0 y 1.0", example = "0.92")
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        Double confidenceScore,

        @Schema(description = "Tiempo total de procesamiento del flujo en n8n en milisegundos", example = "2340")
        Long processingTimeMs,

        @Schema(description = "Lista de articulos o fuentes documentales que respaldan la respuesta (RAG)")
        @Valid
        List<SourceDTO> sources
) {
    // Sub-contrato para cada fuente documental o articulo citado por el flujo RAG de n8n
    @Schema(description = "Fuente documental o articulo de conocimiento citado por la IA")
    public record SourceDTO(
            @Schema(description = "ID opcional del articulo en la tabla knowledge_articles", example = "12")
            Long knowledgeArticleId,

            @Schema(description = "Titulo del manual, guia o ticket de referencia", example = "IPSec VPN Runbook v3.1")
            @NotBlank(message = "Source title is required")
            String sourceTitle,

            @Schema(description = "URL o identificador del documento en la base de conocimiento", example = "https://kb.company.internal/networking/vpn-error-809")
            String referenceUrl,

            @Schema(description = "Fragmento textual extraido por el flujo RAG en n8n", example = "Error 809 occurs when IKE packets are blocked by NAT-T...")
            String citedSnippet
    ) {}
}

