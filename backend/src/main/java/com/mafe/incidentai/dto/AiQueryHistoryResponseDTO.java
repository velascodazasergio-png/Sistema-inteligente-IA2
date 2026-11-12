package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.QueryStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

// Contrato DTO de salida para consultar el historial completo de consultas IA (AiQuery) de un incidente
@Schema(description = "Historial detallado de una consulta enviada a la IA, incluyendo su respuesta, fuentes y calificacion")
public record AiQueryHistoryResponseDTO(
        Long aiQueryId,
        Long incidentId,
        String incidentTitle,
        String sentPrompt,
        QueryStatus queryStatus,
        LocalDateTime queriedAt,
        AiResponseSummaryDTO aiResponse
) {
    // Resumen de la respuesta generada por n8n asociada a la consulta
    public record AiResponseSummaryDTO(
            Long aiResponseId,
            String suggestedDiagnosis,
            String solutionSteps,
            Double confidenceScore,
            Long processingTimeMs,
            LocalDateTime respondedAt,
            List<SourceSummaryDTO> sources,
            RatingSummaryDTO rating
    ) {}

    // Resumen de cada fuente documental con su puntaje de similitud semantica
    public record SourceSummaryDTO(
            Long id,
            Long knowledgeArticleId,
            String sourceTitle,
            String referenceUrl,
            String citedSnippet,
            Double similarityScore
    ) {}

    // Resumen de la calificacion otorgada por el tecnico o usuario
    public record RatingSummaryDTO(
            Long id,
            Integer score,
            Boolean helpful,
            String feedbackComment,
            LocalDateTime ratedAt
    ) {}
}
