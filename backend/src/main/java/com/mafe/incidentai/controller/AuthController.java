package com.mafe.incidentai.controller;

import com.mafe.incidentai.dto.LoginRequestDTO;
import com.mafe.incidentai.dto.LoginResponseDTO;
import com.mafe.incidentai.dto.UserSummaryDTO;
import com.mafe.incidentai.security.AuthService;
import com.mafe.incidentai.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Inicio de sesion (JWT) y perfil del usuario autenticado
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Inicio de sesion con JWT y perfil del usuario autenticado")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    @Operation(summary = "Log in and get a JWT",
            description = "Devuelve un token Bearer. Enviarlo en el encabezado Authorization: Bearer <token>.")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sesion iniciada"),
        @ApiResponse(responseCode = "400", description = "Correo o contrasena con formato invalido"),
        @ApiResponse(responseCode = "401", description = "Correo o contrasena incorrectos")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Get the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario de la sesion"),
        @ApiResponse(responseCode = "401", description = "Sin sesion o token vencido")
    })
    @GetMapping("/me")
    public ResponseEntity<UserSummaryDTO> me() {
        return ResponseEntity.ok(authService.profile(currentUser.get().id()));
    }
}
