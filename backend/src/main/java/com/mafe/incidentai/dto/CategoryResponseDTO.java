package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Technical category used to fill the frontend selectors and filters
@Schema(description = "Technical category of incidents and knowledge articles")
public record CategoryResponseDTO(
        Long id,
        String name,
        String description,
        Boolean active
) {}
