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

// Login (JWT) and profile of the authenticated user
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "JWT login and profile of the authenticated user")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;

    @Operation(summary = "Log in and get a JWT",
            description = "Returns a Bearer token. Send it in the header Authorization: Bearer <token>.")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logged in"),
        @ApiResponse(responseCode = "400", description = "Email or password with an invalid format"),
        @ApiResponse(responseCode = "401", description = "Wrong email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Get the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User of the session"),
        @ApiResponse(responseCode = "401", description = "No session or expired token")
    })
    @GetMapping("/me")
    public ResponseEntity<UserSummaryDTO> me() {
        return ResponseEntity.ok(authService.profile(currentUser.get().id()));
    }
}
