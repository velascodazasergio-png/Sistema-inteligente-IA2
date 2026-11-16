package com.mafe.incidentai.security;

import com.mafe.incidentai.repository.UserSessionRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;

// Accepts a JWT only if its session (jti) is still open in the database: not closed and not expired.
// A token without a session id, or whose session was closed at logout, is rejected with 401.
public class ActiveSessionValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error CLOSED_SESSION =
            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "The session is closed or expired", null);

    private final UserSessionRepository sessionRepository;

    // Injects the session repository
    public ActiveSessionValidator(UserSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    // Looks up the session of the token in the database
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String sessionId = jwt.getId();
        boolean active = sessionId != null
                && sessionRepository.existsByIdAndRevokedAtIsNullAndExpiresAtAfter(sessionId, LocalDateTime.now());
        return active ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(CLOSED_SESSION);
    }
}
