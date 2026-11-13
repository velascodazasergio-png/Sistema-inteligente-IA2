package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// Articulo de la base de conocimiento con sus secciones, categoria, autor y estado.
// problem, symptoms, cause y solution pueden ser nulos en articulos antiguos: en ese caso usar content.
@Schema(description = "Articulo tecnico de la base de conocimiento")
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
