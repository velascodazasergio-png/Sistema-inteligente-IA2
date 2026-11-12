package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

// Contrato DTO inmutable (Java record) para solicitar el analisis de un incidente hacia el webhook de n8n
@Schema(description = "Contrato REST para solicitar analisis de un incidente tecnologico hacia n8n")
public record AiAssistanceRequestDTO(

        @Schema(description = "ID unico del incidente registrado en el sistema", example = "101")
        @NotNull(message = "Incident ID is required")
        Long incidentId,

        @Schema(description = "Titulo breve del incidente", example = "Intermittent corporate VPN tunnel drop")
        @NotBlank(message = "Incident title cannot be blank")
        String incidentTitle,

        @Schema(description = "Descripcion tecnica detallada del problema reportado", example = "Remote users receive error 809 when connecting from Windows 11 clients")
        @NotBlank(message = "Detailed description is required")
        @Size(min = 20, message = "Detailed description must contain at least 20 characters")
        String detailedDescription,

        @Schema(description = "Nombre de la categoria del incidente", example = "Networking & Connectivity")
        @NotBlank(message = "Category name is required")
        String categoryName,

        @Schema(description = "Nivel de prioridad del incidente", example = "HIGH")
        String priority,

        @Schema(description = "Contexto adicional para refinar el prompt enviado al agente en n8n", example = "Check if this matches yesterday's firewall certificate update")
        String additionalPromptContext
) {}

