package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

// Public data of a user for selectors (reporter, evaluator, author)
@Schema(description = "Summary of a system user")
public record UserSummaryDTO(
        Long id,
        String fullName,
        String email,
        UserRole role
) {}
