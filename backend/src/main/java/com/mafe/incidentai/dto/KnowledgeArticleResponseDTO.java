package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// Knowledge base article with its sections, category, author and status.
// problem, symptoms, cause and solution can be null in older articles: use content in that case.
@Schema(description = "Technical knowledge base article")
public record KnowledgeArticleResponseDTO(
        Long id,
        String title,
        String content,
        String problem,
        String symptoms,
        String cause,
        String solution,
        String referenceUrl,
        Boolean active,
        Boolean verified,
        Long categoryId,
        String categoryName,
        Long authorUserId,
        String authorFullName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
