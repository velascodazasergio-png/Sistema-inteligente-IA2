package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Input DTO to rate how useful an AI-generated response was
@Schema(description = "Request to record the human rating of an AI response")
public record CreateRatingRequestDTO(

        @Schema(description = "ID of the user or technician who rates the response", example = "1")
        @NotNull(message = "Evaluator user ID is required")
        Long evaluatorUserId,

        @Schema(description = "Score on a 1 to 5 star scale", example = "5")
        @NotNull(message = "Score is required")
        @Min(value = 1, message = "Minimum score is 1")
        @Max(value = 5, message = "Maximum score is 5")
        Integer score,

        @Schema(description = "Whether the AI suggestion helped to resolve the incident", example = "true")
        @NotNull(message = "Helpful indicator is required")
        Boolean helpful,

        @Schema(description = "Optional technical feedback comment", example = "The suggested firewall rule check solved the VPN issue immediately")
        @Size(max = 500, message = "Feedback comment cannot exceed 500 characters")
        String feedbackComment
) {}

