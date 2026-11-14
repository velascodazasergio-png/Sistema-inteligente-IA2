package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Credenciales para iniciar sesion
@Schema(description = "Correo y contrasena del usuario")
public record LoginRequestDTO(
        @Schema(example = "sarah.jenkins@enterprise.io")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 120, message = "Email cannot exceed 120 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(max = 72, message = "Password cannot exceed 72 characters")
        String password
) {}
