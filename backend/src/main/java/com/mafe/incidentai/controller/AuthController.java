package com.mafe.incidentai.controller;

import com.mafe.incidentai.dto.LoginRequestDTO;
import com.mafe.incidentai.dto.LoginResponseDTO;
import com.mafe.incidentai.dto.UserPreferencesDTO;
import com.mafe.incidentai.dto.UserSummaryDTO;
import com.mafe.incidentai.security.AuthService;
import com.mafe.incidentai.security.CurrentUser;
import com.mafe.incidentai.service.UserPreferencesService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Login and logout (sessions stored in the database), profile and preferences of the authenticated user
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and logout (sessions stored in the database), profile and preferences")
public class AuthController {

    private final AuthService authService;
    private final CurrentUser currentUser;
    private final UserPreferencesService preferencesService;

    // POST /auth/login: checks the credentials, opens a session in the database and returns its JWT
    @Operation(summary = "Log in and get a JWT",
            description = "Opens a session in the database and returns a Bearer token that points to it. "
                    + "Send it in the header Authorization: Bearer <token>.")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logged in"),
        @ApiResponse(responseCode = "400", description = "Email or password with an invalid format"),
        @ApiResponse(responseCode = "401", description = "Wrong email or password")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request,
                                                  @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        return ResponseEntity.ok(authService.login(request, userAgent));
    }

    // POST /auth/logout: closes the session in the database; its token stops working
    @Operation(summary = "Log out", description = "Closes the current session in the database.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Session closed"),
        @ApiResponse(responseCode = "401", description = "No session or expired token")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        currentUser.sessionId().ifPresent(authService::logout);
        return ResponseEntity.noContent().build();
    }

    // GET /auth/me: profile of the logged-in user
    @Operation(summary = "Get the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User of the session"),
        @ApiResponse(responseCode = "401", description = "No session or expired token")
    })
    @GetMapping("/me")
    public ResponseEntity<UserSummaryDTO> me() {
        return ResponseEntity.ok(authService.profile(currentUser.get().id()));
    }

    // GET /auth/me/preferences: interface preferences stored for the logged-in user
    @Operation(summary = "Get the preferences of the authenticated user")
    @GetMapping("/me/preferences")
    public ResponseEntity<UserPreferencesDTO> preferences() {
        return ResponseEntity.ok(preferencesService.get(currentUser.get().id()));
    }

    // PUT /auth/me/preferences: saves the preferences that were sent (null fields do not change)
    @Operation(summary = "Save the preferences of the authenticated user", description = "Null fields do not change.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferences saved"),
        @ApiResponse(responseCode = "400", description = "Invalid value")
    })
    @PutMapping("/me/preferences")
    public ResponseEntity<UserPreferencesDTO> updatePreferences(@Valid @RequestBody UserPreferencesDTO changes) {
        return ResponseEntity.ok(preferencesService.update(currentUser.get().id(), changes));
    }
}
