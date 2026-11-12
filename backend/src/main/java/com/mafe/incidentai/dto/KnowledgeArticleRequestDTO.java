package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Datos para crear o editar un articulo de la base de conocimiento (RF02)
@Schema(description = "Datos para crear o editar un articulo tecnico de la base de conocimiento")
public record KnowledgeArticleRequestDTO(
        @Schema(description = "Titulo del articulo", example = "IPSec VPN Error 809 Troubleshooting Runbook")
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 180, message = "Title must be between 5 and 180 characters")
        String title,

        @Schema(description = "Contenido tecnico: problema, sintomas, causa y solucion", example = "Verify UDP ports 500 and 4500 on perimeter firewall and restart IKEEXT.")
        @NotBlank(message = "Content is required")
        @Size(min = 20, message = "Content must contain at least 20 characters")
        String content,

        @Schema(description = "URL de referencia opcional (http o https)", example = "https://kb.enterprise.io/net/vpn-error-809")
        @Size(max = 500, message = "Reference URL cannot exceed 500 characters")
        @Pattern(regexp = "^$|^https?://.+", message = "Reference URL must start with http:// or https://")
        String referenceUrl,

        @Schema(description = "ID de la categoria tecnica", example = "1")
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @Schema(description = "ID del usuario que redacta el articulo (opcional)", example = "2")
        Long authorUserId
) {}

