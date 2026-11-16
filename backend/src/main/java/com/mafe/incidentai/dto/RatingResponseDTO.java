package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// Output DTO with the detail of the rating recorded for an AI response
@Schema(description = "Response with the data of the recorded rating")
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

