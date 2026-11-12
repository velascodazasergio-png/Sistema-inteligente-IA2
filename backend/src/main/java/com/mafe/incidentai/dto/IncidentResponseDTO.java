package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.IncidentStatus;
import com.mafe.incidentai.domain.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

// Contrato DTO de salida para devolver la informacion de un incidente sin exponer la entidad JPA
@Schema(description = "Respuesta con el detalle de un incidente registrado")
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
        LocalDateTime updatedAt
) {}

