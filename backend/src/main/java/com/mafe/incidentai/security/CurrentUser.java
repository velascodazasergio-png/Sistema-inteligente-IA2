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

// User making the current request, read from the JWT (or from the database if the session does not come from a JWT)
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

    // Empty when there is no user: an anonymous request or a service (n8n with X-Callback-Token)
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
