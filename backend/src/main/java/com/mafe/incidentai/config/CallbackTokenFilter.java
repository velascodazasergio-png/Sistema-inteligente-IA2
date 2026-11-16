package com.mafe.incidentai.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

// Protects the callbacks that only n8n may call (response and failure of an AI query).
// If app.callback.token is empty, the check is disabled (development mode).
@Component
public class CallbackTokenFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Callback-Token";

    private static final Pattern PROTECTED_PATHS =
            Pattern.compile("^/api/v1/ai-assistance/(responses|queries/\\d+/failure)/?$");

    private final byte[] expectedToken;

    public CallbackTokenFilter(@Value("${app.callback.token:}") String token) {
        this.expectedToken = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return expectedToken.length == 0
                || !HttpMethod.POST.matches(request.getMethod())
                || !PROTECTED_PATHS.matcher(request.getRequestURI()).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String received = request.getHeader(HEADER);
        // Constant-time comparison so the token does not leak through timing
        if (received != null && MessageDigest.isEqual(expectedToken, received.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"status\":401,"
                + "\"error\":\"Unauthorized\",\"message\":\"Missing or invalid " + HEADER + " header\"}");
    }
}
