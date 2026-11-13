package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Datos para crear o editar un articulo de la base de conocimiento (RF02).
// El contenido completo (content) lo arma el backend a partir de las secciones.
@Schema(description = "Datos para crear o editar un articulo tecnico de la base de conocimiento")
public record KnowledgeArticleRequestDTO(
        @Schema(description = "Titulo del articulo", example = "IPSec VPN Error 809 Troubleshooting Runbook")
        @NotBlank(message = "Title is required")
        @Size(min = 5, max = 180, message = "Title must be between 5 and 180 characters")
        String title,

        @Schema(description = "Descripcion del problema", example = "Remote users cannot establish the corporate VPN tunnel.")
        @NotBlank(message = "Problem is required")
        @Size(min = 10, max = 4000, message = "Problem must be between 10 and 4000 characters")
        String problem,

        @Schema(description = "Sintomas observables (opcional)", example = "Windows 11 shows error 809 when connecting.")
        @Size(max = 4000, message = "Symptoms cannot exceed 4000 characters")
        String symptoms,

        @Schema(description = "Causa raiz (opcional)", example = "The perimeter firewall blocks UDP 500 and 4500 (NAT-T).")
        @Size(max = 4000, message = "Cause cannot exceed 4000 characters")
        String cause,

        @Schema(description = "Solucion o pasos para resolverlo", example = "1. Allow UDP 500/4500 on the firewall. 2. Restart IKEEXT on the client.")
        @NotBlank(message = "Solution is required")
        @Size(min = 10, max = 8000, message = "Solution must be between 10 and 8000 characters")
        String solution,

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
