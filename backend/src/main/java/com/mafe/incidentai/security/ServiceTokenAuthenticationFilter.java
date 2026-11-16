package com.mafe.incidentai.security;

import com.mafe.incidentai.config.CallbackTokenFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

// Identifies n8n with the SERVICE role when it sends the same X-Callback-Token it uses for callbacks.
// This way n8n can read the articles to index them without a user or a JWT.
// A missing or wrong token does not reject the request here: it just does not authenticate it.
class ServiceTokenAuthenticationFilter extends OncePerRequestFilter {

    private final byte[] expectedToken;

    ServiceTokenAuthenticationFilter(String token) {
        this.expectedToken = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return expectedToken.length == 0 || request.getHeader(CallbackTokenFilter.HEADER) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        byte[] received = request.getHeader(CallbackTokenFilter.HEADER).getBytes(StandardCharsets.UTF_8);
        if (MessageDigest.isEqual(expectedToken, received)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            var authentication = new UsernamePasswordAuthenticationToken(
                    "n8n", null, AuthorityUtils.createAuthorityList("ROLE_" + SecurityConfig.SERVICE));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
    }
}
