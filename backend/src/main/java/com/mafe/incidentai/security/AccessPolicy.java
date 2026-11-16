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

// Access rules that depend on the resource (not only on the route): an employee only works with their own incidents
// and only reads published articles. Administrators and technicians have no such restrictions.
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

    // true if the request comes from a person with a session (not n8n and not an anonymous request)
    public boolean isAuthenticatedUser() {
        return currentUser.find().isPresent();
    }

    public boolean isEmployee() {
        return user().is(UserRole.EMPLOYEE);
    }

    public boolean isAdmin() {
        return user().is(UserRole.ADMIN);
    }

    // Throws 403 if an employee tries to see another person's incident
    public void checkIncident(IncidentResponseDTO incident) {
        var me = user();
        if (me.is(UserRole.EMPLOYEE) && !Objects.equals(incident.userId(), me.id())) {
            throw new AccessDeniedException(NOT_YOUR_INCIDENT);
        }
    }

    public void checkIncident(Long incidentId) {
        if (isEmployee()) checkIncident(incidentService.getIncident(incidentId));
    }

    // IDs of the current employee's incidents (to filter their AI history)
    public Set<Long> ownIncidentIds() {
        return incidentService.filterIncidents(null, null, null, user().id(), null, null)
                .stream().map(IncidentResponseDTO::id).collect(Collectors.toSet());
    }

    // An employee can only read active and verified articles (the same ones the AI uses)
    public void checkArticle(KnowledgeArticleResponseDTO article) {
        if (isEmployee() && !(Boolean.TRUE.equals(article.active()) && Boolean.TRUE.equals(article.verified()))) {
            throw new AccessDeniedException(ARTICLE_NOT_PUBLISHED);
        }
    }
}
