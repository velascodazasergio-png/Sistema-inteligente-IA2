package com.mafe.incidentai.dto;

import com.mafe.incidentai.domain.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Contrato DTO de entrada para registrar un nuevo incidente tecnologico
@Schema(description = "Datos requeridos para registrar un nuevo incidente en el sistema")
public record CreateIncidentRequestDTO(

        @Schema(description = "Titulo descriptivo del incidente", example = "Database connection pool exhausted in ERP")
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 150, message = "Title must be between 5 and 150 characters")
        String title,

        @Schema(description = "Descripcion detallada del sintoma o error tecnico", example = "HikariPool-1 timeouts occurring every 10 minutes on production node 2")
        @NotBlank(message = "Description is required")
        @Size(min = 20, max = 4000, message = "Description must be between 20 and 4000 characters")
        String description,

        @Schema(description = "Nivel de prioridad del incidente", example = "CRITICAL")
        @NotNull(message = "Priority is required")
        Priority priority,

        @Schema(description = "ID del usuario que reporta el incidente", example = "1")
        @NotNull(message = "User ID is required")
        Long userId,

        @Schema(description = "ID de la categoria tecnica asociada", example = "2")
        @NotNull(message = "Category ID is required")
        Long categoryId
) {}

