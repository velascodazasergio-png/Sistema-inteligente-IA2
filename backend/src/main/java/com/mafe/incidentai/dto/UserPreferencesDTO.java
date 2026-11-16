package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

// Interface preferences of the logged-in user. On update, null fields are left unchanged.
@Schema(description = "Interface preferences of the logged-in user, stored in the database")
public record UserPreferencesDTO(
        @Schema(description = "true = collapsed side menu; null = automatic", example = "false")
        Boolean sidebarCollapsed,

        @Pattern(regexp = "cards|table", message = "Knowledge view must be cards or table")
        @Schema(description = "How the knowledge base is shown", example = "cards")
        String knowledgeView
) {}
