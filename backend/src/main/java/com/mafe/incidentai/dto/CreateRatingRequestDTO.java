package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Contrato DTO de entrada para calificar la utilidad de una respuesta generada por la IA
@Schema(description = "Solicitud para registrar la calificacion humana sobre una respuesta de la IA")
public record CreateRatingRequestDTO(

        @Schema(description = "ID del usuario o tecnico que evalua la respuesta", example = "1")
        @NotNull(message = "Evaluator user ID is required")
        Long evaluatorUserId,

        @Schema(description = "Puntuacion en escala de 1 a 5 estrellas", example = "5")
        @NotNull(message = "Score is required")
        @Min(value = 1, message = "Minimum score is 1")
        @Max(value = 5, message = "Maximum score is 5")
        Integer score,

        @Schema(description = "Indica si la sugerencia de la IA fue util para resolver el incidente", example = "true")
        @NotNull(message = "Helpful indicator is required")
        Boolean helpful,

        @Schema(description = "Comentario opcional de retroalimentacion tecnica", example = "The suggested firewall rule check solved the VPN issue immediately")
        @Size(max = 500, message = "Feedback comment cannot exceed 500 characters")
        String feedbackComment
) {}

