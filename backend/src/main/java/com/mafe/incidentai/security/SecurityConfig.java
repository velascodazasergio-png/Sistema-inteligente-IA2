package com.mafe.incidentai.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.extern.slf4j.Slf4j;
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

// Seguridad de la API: sesiones sin estado con JWT (HS256) y permisos por rol.
//
// Roles (enum UserRole):
//   ADMIN              -> todo, incluida la verificacion de articulos.
//   SUPPORT_TECHNICIAN -> gestiona incidentes y articulos, dashboard y analitica.
//   EMPLOYEE           -> solo sus propios incidentes, articulos publicados y su historial de IA.
// La propiedad de los recursos del empleado se valida en AccessPolicy (controladores).
// n8n no inicia sesion: se identifica con X-Callback-Token (ServiceTokenAuthenticationFilter).
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
            // Sin JWT_SECRET: clave aleatoria. Funciona, pero las sesiones se pierden al reiniciar el backend.
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            log.warn("JWT_SECRET no esta configurado: se usa una clave aleatoria y las sesiones se pierden al reiniciar.");
        } else {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres (HS256).");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // Valida expiracion (exp) y que el token lo haya emitido esta API (iss)
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(JwtService.ISSUER)));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // El claim "role" del token se convierte en la autoridad ROLE_<rol>
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
                // API sin estado: no hay cookies de sesion, por lo que CSRF no aplica
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
                            // Publicos: inicio de sesion, documentacion, consola H2 y preflight CORS
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                            .requestMatchers("/api-docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                    "/h2-console/**", "/error").permitAll()
                            // Callbacks de n8n: los protege CallbackTokenFilter con X-Callback-Token
                            .requestMatchers(HttpMethod.POST, "/api/v1/ai-assistance/responses",
                                    "/api/v1/ai-assistance/queries/*/failure").permitAll();

                    // Lectura de articulos: n8n la usa para indexar y validar fuentes
                    if (serviceTokenConfigured) {
                        auth.requestMatchers(HttpMethod.GET, "/api/v1/knowledge-articles", "/api/v1/knowledge-articles/*")
                                .hasAnyRole(ADMIN, TECH, EMPLOYEE, SERVICE);
                    } else {
                        // Sin CALLBACK_TOKEN (modo desarrollo) n8n no puede identificarse: la lectura queda abierta
                        log.warn("CALLBACK_TOKEN vacio: la lectura de articulos queda abierta para que n8n pueda indexar.");
                        auth.requestMatchers(HttpMethod.GET, "/api/v1/knowledge-articles", "/api/v1/knowledge-articles/*")
                                .permitAll();
                    }

                    auth
                            // Base de conocimiento: crear y editar (admin, tecnico); verificar o activar (solo admin)
                            .requestMatchers(HttpMethod.PATCH, "/api/v1/knowledge-articles/*/status").hasRole(ADMIN)
                            .requestMatchers("/api/v1/knowledge-articles/**").hasAnyRole(ADMIN, TECH)
                            // Analitica y dashboard
                            .requestMatchers("/api/v1/analytics/**").hasAnyRole(ADMIN, TECH)
                            // Directorio de usuarios (selectores de responsable, autor y evaluador)
                            .requestMatchers("/api/v1/users/**").hasAnyRole(ADMIN, TECH)
                            // Editar un incidente o cambiar su estado: personal de soporte
                            .requestMatchers(HttpMethod.PUT, "/api/v1/incidents/*").hasAnyRole(ADMIN, TECH)
                            .requestMatchers(HttpMethod.PATCH, "/api/v1/incidents/*/status").hasAnyRole(ADMIN, TECH)
                            // Incidentes, asistente IA, categorias y sesion: cualquier usuario con sesion.
                            // El empleado solo ve lo suyo (AccessPolicy).
                            .requestMatchers("/api/v1/incidents/**", "/api/v1/ai-assistance/**", "/api/v1/categories/**",
                                    "/api/v1/auth/**").hasAnyRole(ADMIN, TECH, EMPLOYEE)
                            .anyRequest().authenticated();
                });
        return http.build();
    }
}
