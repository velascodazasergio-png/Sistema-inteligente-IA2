package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Categoria tecnica para llenar selectores y filtros del frontend
@Schema(description = "Categoria tecnica de incidentes y articulos de conocimiento")
public record CategoryResponseDTO(
        Long id,
        String name,
        String description,
        Boolean active
) {}

