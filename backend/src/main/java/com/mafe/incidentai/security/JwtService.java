package com.mafe.incidentai.security;

import com.mafe.incidentai.domain.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

// Emite los JWT de sesion. Claims: sub (correo), uid (id del usuario), role y name.
@Service
public class JwtService {

    public static final String ISSUER = "incident-ai-api";
    public static final String ROLE_CLAIM = "role";
    public static final String USER_ID_CLAIM = "uid";

    private final JwtEncoder encoder;
    private final Duration lifetime;

    public JwtService(JwtEncoder encoder, @Value("${app.jwt.expiration-minutes:480}") long expirationMinutes) {
        this.encoder = encoder;
        this.lifetime = Duration.ofMinutes(expirationMinutes);
    }

    public record IssuedToken(String value, Instant expiresAt) {}

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(lifetime);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim(USER_ID_CLAIM, user.getId())
                .claim(ROLE_CLAIM, user.getRole().name())
                .claim("name", user.getFullName())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }
}
