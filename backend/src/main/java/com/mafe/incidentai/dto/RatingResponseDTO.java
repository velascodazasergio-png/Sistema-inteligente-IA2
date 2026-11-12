package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// Contrato DTO de salida con el detalle de la calificacion registrada sobre una respuesta de IA
@Schema(description = "Respuesta con los datos de la calificacion registrada")
public record RatingResponseDTO(
        Long id,
        Long aiResponseId,
        Long evaluatorUserId,
        String evaluatorFullName,
        Integer score,
        Boolean helpful,
        String feedbackComment,
        LocalDateTime ratedAt
) {}

