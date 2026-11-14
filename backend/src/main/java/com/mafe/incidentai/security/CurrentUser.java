package com.mafe.incidentai.security;

import com.mafe.incidentai.domain.enums.UserRole;
import com.mafe.incidentai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Usuario que hace la peticion actual, leido del JWT (o de la base de datos si la sesion no viene de un JWT)
@Component
@RequiredArgsConstructor
public class CurrentUser {

    public record AuthUser(Long id, String email, UserRole role) {
        public boolean is(UserRole other) {
            return role == other;
        }
    }

    private final UserRepository userRepository;

    public AuthUser get() {
        return find().orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Authentication required"));
    }

    // Vacio si no hay usuario: peticion anonima o de un servicio (n8n con X-Callback-Token)
    public Optional<AuthUser> find() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            var jwt = jwtAuth.getToken();
            Object uid = jwt.getClaim(JwtService.USER_ID_CLAIM);
            String role = jwt.getClaimAsString(JwtService.ROLE_CLAIM);
            if (uid instanceof Number number && role != null) {
                return Optional.of(new AuthUser(number.longValue(), jwt.getSubject(), UserRole.valueOf(role)));
            }
        }
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return userRepository.findByEmail(auth.getName())
                    .map(u -> new AuthUser(u.getId(), u.getEmail(), u.getRole()));
        }
        return Optional.empty();
    }
}
