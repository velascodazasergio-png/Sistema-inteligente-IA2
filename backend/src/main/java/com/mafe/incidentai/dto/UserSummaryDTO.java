package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.entity.User;
import com.mafe.incidentai.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

// Public data of a user for selectors (reporter, evaluator, author)
@Schema(description = "Summary of a system user")
public record UserSummaryDTO(
        Long id,
        String fullName,
        String email,
        UserRole role,
        @Schema(description = "Department or area of the user", example = "Finance")
        String area
) {
    // Maps a User entity to its public summary
    public static UserSummaryDTO from(User u) {
        return new UserSummaryDTO(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.getArea());
    }
}
