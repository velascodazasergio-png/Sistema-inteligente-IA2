package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Failure report that n8n sends when it cannot process a query (embeddings, Qdrant or LLM)
@Schema(description = "AI workflow failure reported by n8n")
public record AiFailureReportDTO(
        @Schema(description = "Query ID (must match the path; optional)", example = "6")
        Long aiQueryId,

        @Schema(description = "Failure code (sent by n8n)", example = "EMBEDDING_FALLO")
        @NotBlank(message = "Error code is required")
        @Size(max = 60, message = "Error code cannot exceed 60 characters")
        String errorCode,

        @Schema(description = "Human-readable failure message", example = "Could not generate the query embedding.")
        @NotBlank(message = "Message is required")
        @Size(max = 500, message = "Message cannot exceed 500 characters")
        String message,

        @Schema(description = "Technical detail (never shown to the end user)", example = "ETIMEDOUT")
        @Size(max = 500, message = "Technical detail cannot exceed 500 characters")
        String technicalDetail,

        @Schema(description = "Time spent in n8n before the failure", example = "1200")
        Long processingTimeMs
) {}
