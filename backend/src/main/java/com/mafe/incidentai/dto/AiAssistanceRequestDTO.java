package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

// Immutable DTO (Java record) to request the analysis of an incident through the n8n webhook
@Schema(description = "REST contract to request the analysis of a technology incident in n8n")
public record AiAssistanceRequestDTO(

        @Schema(description = "Unique ID of the incident registered in the system", example = "101")
        @NotNull(message = "Incident ID is required")
        Long incidentId,

        @Schema(description = "Short title of the incident", example = "Intermittent corporate VPN tunnel drop")
        @NotBlank(message = "Incident title cannot be blank")
        String incidentTitle,

        @Schema(description = "Detailed technical description of the reported problem", example = "Remote users receive error 809 when connecting from Windows 11 clients")
        @NotBlank(message = "Detailed description is required")
        @Size(min = 20, message = "Detailed description must contain at least 20 characters")
        String detailedDescription,

        @Schema(description = "Name of the incident category", example = "Networking & Connectivity")
        @NotBlank(message = "Category name is required")
        String categoryName,

        @Schema(description = "Priority level of the incident", example = "HIGH")
        String priority,

        @Schema(description = "Additional context to refine the prompt sent to the agent in n8n", example = "Check if this matches yesterday's firewall certificate update")
        @Size(max = 500, message = "Additional context cannot exceed 500 characters")
        String additionalPromptContext
) {}

