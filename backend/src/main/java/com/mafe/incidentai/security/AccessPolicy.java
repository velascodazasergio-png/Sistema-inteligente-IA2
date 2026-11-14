package com.mafe.incidentai.security;

import com.mafe.incidentai.domain.enums.UserRole;
import com.mafe.incidentai.dto.IncidentResponseDTO;
import com.mafe.incidentai.dto.KnowledgeArticleResponseDTO;
import com.mafe.incidentai.service.IncidentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// Reglas de acceso que dependen del recurso (no solo de la ruta): un empleado solo trabaja con sus incidentes
// y solo lee articulos publicados. Administradores y tecnicos no tienen estas restricciones.
@Component
@RequiredArgsConstructor
public class AccessPolicy {

    public static final String NOT_YOUR_INCIDENT = "You can only access your own incidents";
    public static final String ARTICLE_NOT_PUBLISHED = "This article is not published";

    private final CurrentUser currentUser;
    private final IncidentService incidentService;

    public CurrentUser.AuthUser user() {
        return currentUser.get();
    }

    // true si la peticion la hace una persona con sesion (no n8n ni una peticion anonima)
    public boolean isAuthenticatedUser() {
        return currentUser.find().isPresent();
    }

    public boolean isEmployee() {
        return user().is(UserRole.EMPLOYEE);
    }

    public boolean isAdmin() {
        return user().is(UserRole.ADMIN);
    }

    // Lanza 403 si un empleado intenta ver un incidente de otra persona
    public void checkIncident(IncidentResponseDTO incident) {
        var me = user();
        if (me.is(UserRole.EMPLOYEE) && !Objects.equals(incident.userId(), me.id())) {
            throw new AccessDeniedException(NOT_YOUR_INCIDENT);
        }
    }

    public void checkIncident(Long incidentId) {
        if (isEmployee()) checkIncident(incidentService.getIncident(incidentId));
    }

    // Ids de los incidentes del empleado actual (para filtrar su historial de IA)
    public Set<Long> ownIncidentIds() {
        return incidentService.filterIncidents(null, null, null, user().id(), null, null)
                .stream().map(IncidentResponseDTO::id).collect(Collectors.toSet());
    }

    // Un empleado solo puede leer articulos activos y verificados (los mismos que usa la IA)
    public void checkArticle(KnowledgeArticleResponseDTO article) {
        if (isEmployee() && !(Boolean.TRUE.equals(article.active()) && Boolean.TRUE.equals(article.verified()))) {
            throw new AccessDeniedException(ARTICLE_NOT_PUBLISHED);
        }
    }
}
