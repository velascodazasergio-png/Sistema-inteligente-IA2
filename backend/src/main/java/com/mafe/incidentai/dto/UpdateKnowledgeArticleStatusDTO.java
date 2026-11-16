package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Partial change of the flags of an article. Null fields are left unchanged.
@Schema(description = "Activates/deactivates or verifies an article. Omitted fields do not change.")
public record UpdateKnowledgeArticleStatusDTO(
        @Schema(description = "true to activate, false to deactivate", example = "true")
        Boolean active,

        @Schema(description = "true to mark it as verified", example = "true")
        Boolean verified
) {}
