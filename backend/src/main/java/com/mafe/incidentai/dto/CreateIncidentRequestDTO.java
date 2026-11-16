package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Input DTO to register a new technology incident
@Schema(description = "Data required to register a new incident in the system")
public record CreateIncidentRequestDTO(

        @Schema(description = "Descriptive title of the incident", example = "Database connection pool exhausted in ERP")
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
        String title,

        @Schema(description = "Detailed description of the symptom or technical error", example = "HikariPool-1 timeouts occurring every 10 minutes on production node 2")
        @NotBlank(message = "Description is required")
        @Size(min = 20, max = 4000, message = "Description must be between 20 and 4000 characters")
        String description,

        @Schema(description = "Priority level of the incident", example = "CRITICAL")
        @NotNull(message = "Priority is required")
        Priority priority,

        @Schema(description = "ID of the user who reports the incident", example = "1")
        @NotNull(message = "User ID is required")
        Long userId,

        @Schema(description = "ID of the related technical category", example = "2")
        @NotNull(message = "Category ID is required")
        Long categoryId
) {}

