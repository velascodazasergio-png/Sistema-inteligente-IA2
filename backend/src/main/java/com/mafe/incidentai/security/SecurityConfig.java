package com.mafe.incidentai.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.extern.slf4j.Slf4j;
import com.mafe.incidentai.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

// API security: stateless sessions with JWT (HS256) and role-based permissions.
//
// Roles (UserRole enum):
//   ADMIN              -> everything, including article verification.
//   SUPPORT_TECHNICIAN -> manages incidents and articles, dashboard and analytics.
//   EMPLOYEE           -> only their own incidents, published articles and their AI history.
// Ownership of the employee's resources is checked in AccessPolicy (controllers).
// n8n does not log in: it identifies itself with X-Callback-Token (ServiceTokenAuthenticationFilter).
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    static final String ADMIN = "ADMIN";
    static final String TECH = "SUPPORT_TECHNICIAN";
    static final String EMPLOYEE = "EMPLOYEE";
    static final String SERVICE = "SERVICE";

    @Bean
    SecretKey jwtSigningKey(@Value("${app.jwt.secret:}") String secret) {
        byte[] bytes;
        if (secret == null || secret.isBlank()) {
            // No JWT_SECRET: random key. It works, but sessions are lost when the backend restarts.
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            log.warn("JWT_SECRET is not set: using a random key, so sessions are lost on restart.");
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET must be at least 32 characters long (HS256).");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, UserSessionRepository sessionRepository) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Validates the expiration (exp), that this API issued the token (iss)
        // and that its session is still open in the database (jti)
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(JwtService.ISSUER),
                new ActiveSessionValidator(sessionRepository)));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // The "role" claim of the token becomes the authority ROLE_<role>
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(JwtService.ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JsonSecurityErrorHandler errors,
                                            @Value("${app.callback.token:}") String callbackToken) throws Exception {
        boolean serviceTokenConfigured = callbackToken != null && !callbackToken.isBlank();

        http
                // Stateless API: there are no session cookies, so CSRF does not apply
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin())) // consola H2 en desarrollo
                .addFilterBefore(new ServiceTokenAuthenticationFilter(callbackToken), BearerTokenAuthenticationFilter.class)
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .authorizeHttpRequests(auth -> {
                    auth
                            // Public: login, documentation, H2 console and CORS preflight
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                            .requestMatchers("/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                    "/h2-console/**", "/error").permitAll()
                            // n8n callbacks: protected by CallbackTokenFilter with X-Callback-Token
                            .requestMatchers(HttpMethod.POST, "/api/v1/ai-assistance/responses",
                                    "/api/v1/ai-assistance/queries/*/failure").permitAll();

                    // Reading articles: n8n uses it to index and to validate sources
                    if (serviceTokenConfigured) {
                        auth.requestMatchers(HttpMethod.GET, "/api/v1/knowledge-articles", "/api/v1/knowledge-articles/*")
                                .hasAnyRole(ADMIN, TECH, EMPLOYEE, SERVICE);
                    } else {
                        // Without CALLBACK_TOKEN (development mode) n8n cannot identify itself: reading stays open
                        log.warn("CALLBACK_TOKEN is empty: article reads stay open so n8n can index them.");
                        auth.requestMatchers(HttpMethod.GET, "/api/v1/knowledge-articles", "/api/v1/knowledge-articles/*")
                                .permitAll();
                    }

                    auth
                            // Knowledge base: create and edit (admin, technician); verify or activate (admin only)
                            .requestMatchers(HttpMethod.PATCH, "/api/v1/knowledge-articles/*/status").hasRole(ADMIN)
                            .requestMatchers("/api/v1/knowledge-articles/**").hasAnyRole(ADMIN, TECH)
                            // Analytics and dashboard
                            .requestMatchers("/api/v1/analytics/**").hasAnyRole(ADMIN, TECH)
                            // User directory (selectors for reporter, author and evaluator)
                            .requestMatchers("/api/v1/users/**").hasAnyRole(ADMIN, TECH)
                            // Editing an incident or changing its status: support staff
                            .requestMatchers(HttpMethod.PUT, "/api/v1/incidents/*").hasAnyRole(ADMIN, TECH)
                            .requestMatchers(HttpMethod.PATCH, "/api/v1/incidents/*/status").hasAnyRole(ADMIN, TECH)
                            // Incidents, AI assistant, categories and session: any logged-in user.
                            // Employees only see their own data (AccessPolicy).
                            .requestMatchers("/api/v1/incidents/**", "/api/v1/ai-assistance/**", "/api/v1/categories/**",
                                    "/api/v1/auth/**").hasAnyRole(ADMIN, TECH, EMPLOYEE)
                            .anyRequest().authenticated();
                });
        return http.build();
    }
}
