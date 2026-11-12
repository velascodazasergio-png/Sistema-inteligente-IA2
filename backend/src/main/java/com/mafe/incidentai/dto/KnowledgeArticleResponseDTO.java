package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// Articulo de la base de conocimiento con su categoria, autor y estado
@Schema(description = "Articulo tecnico de la base de conocimiento")
public record KnowledgeArticleResponseDTO(
        Long id,
        String title,
        String content,
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

