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

// Identifica a n8n como el rol SERVICE cuando envia el mismo X-Callback-Token que usa en los callbacks.
// Asi n8n puede leer los articulos para indexarlos sin tener usuario ni JWT.
// Un token ausente o incorrecto no rechaza la peticion aqui: simplemente no la autentica.
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
