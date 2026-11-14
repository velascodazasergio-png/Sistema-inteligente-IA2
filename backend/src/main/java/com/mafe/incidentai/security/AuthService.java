package com.mafe.incidentai.security;

import com.mafe.incidentai.domain.entity.User;
import com.mafe.incidentai.dto.LoginRequestDTO;
import com.mafe.incidentai.dto.LoginResponseDTO;
import com.mafe.incidentai.dto.UserSummaryDTO;
import com.mafe.incidentai.exception.ResourceNotFoundException;
import com.mafe.incidentai.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

// Inicio de sesion con correo y contrasena: devuelve un JWT con el rol del usuario
@Service
public class AuthService {

    public static final String BAD_CREDENTIALS = "Invalid email or password";

    // Hash BCrypt de una contrasena cualquiera: se compara cuando el correo no existe,
    // para que la respuesta tarde lo mismo y no revele que correos estan registrados.
    private final String dummyHash;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("no-user-" + System.nanoTime());
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email).orElse(null);
        String hash = user != null && user.getPasswordHash() != null ? user.getPasswordHash() : dummyHash;
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user == null || user.getPasswordHash() == null || !matches) {
            throw new BadCredentialsException(BAD_CREDENTIALS);
        }
        JwtService.IssuedToken token = jwtService.issue(user);
        return new LoginResponseDTO(token.value(), "Bearer", token.expiresAt(), toSummary(user));
    }

    @Transactional(readOnly = true)
    public UserSummaryDTO profile(Long userId) {
        return userRepository.findById(userId)
                .map(AuthService::toSummary)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    private static UserSummaryDTO toSummary(User u) {
        return new UserSummaryDTO(u.getId(), u.getFullName(), u.getEmail(), u.getRole());
    }
}
