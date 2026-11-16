package com.mafe.incidentai.security;

import com.mafe.incidentai.domain.entity.User;
import com.mafe.incidentai.domain.entity.UserSession;
import com.mafe.incidentai.dto.LoginRequestDTO;
import com.mafe.incidentai.dto.LoginResponseDTO;
import com.mafe.incidentai.dto.UserSummaryDTO;
import com.mafe.incidentai.exception.ResourceNotFoundException;
import com.mafe.incidentai.repository.UserRepository;
import com.mafe.incidentai.repository.UserSessionRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

// Login with email and password: opens a session in the database and returns a JWT that points to it
@Service
public class AuthService {

    public static final String BAD_CREDENTIALS = "Invalid email or password";

    // BCrypt hash of an arbitrary password: it is compared when the email does not exist,
    // so the response takes the same time and does not reveal which emails are registered.
    private final String dummyHash;

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // Injects the repositories, the password encoder and the JWT issuer
    public AuthService(UserRepository userRepository, UserSessionRepository sessionRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("no-user-" + System.nanoTime());
    }

    // Validates email and password, stores the new session and returns a signed JWT with the user data
    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request, String userAgent) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email).orElse(null);
        String hash = user != null && user.getPasswordHash() != null ? user.getPasswordHash() : dummyHash;
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user == null || user.getPasswordHash() == null || !matches) {
            throw new BadCredentialsException(BAD_CREDENTIALS);
        }

        String sessionId = UUID.randomUUID().toString();
        JwtService.IssuedToken token = jwtService.issue(user, sessionId);

        UserSession session = new UserSession();
        session.setId(sessionId);
        session.setUser(user);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.ofInstant(token.expiresAt(), ZoneId.systemDefault()));
        session.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 255)));
        sessionRepository.save(session);

        return new LoginResponseDTO(token.value(), "Bearer", token.expiresAt(), UserSummaryDTO.from(user));
    }

    // Closes a session: its token stops working immediately, on every browser
    @Transactional
    public void logout(String sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.getRevokedAt() == null) {
                session.setRevokedAt(LocalDateTime.now());
            }
        });
    }

    // Public profile of a user
    @Transactional(readOnly = true)
    public UserSummaryDTO profile(Long userId) {
        return userRepository.findById(userId)
                .map(UserSummaryDTO::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }
}
