package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.IncidentStatus;
import com.mafe.incidentai.domain.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// Output DTO that returns an incident without exposing the JPA entity
@Schema(description = "Response with the detail of a registered incident")
public record IncidentResponseDTO(
        Long id,
        String title,
        String description,
        Priority priority,
        IncidentStatus status,
        Long userId,
        String userFullName,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt
) {}
