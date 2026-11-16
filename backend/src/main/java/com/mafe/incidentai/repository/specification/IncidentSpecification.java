package com.mafe.incidentai.repository.specification;

import com.mafe.incidentai.domain.entity.Incident;
import com.mafe.incidentai.domain.enums.IncidentStatus;
import com.mafe.incidentai.domain.enums.Priority;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Utility class that builds dynamic JPA predicates from the filters sent by the user
public final class IncidentSpecification {

    private IncidentSpecification() {
        // Private constructor: utility class, not meant to be instantiated
    }

    public static Specification<Incident> withDynamicFilters(
            Long categoryId,
            Priority priority,
            IncidentStatus status,
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. If categoryId was sent, filter by the category foreign key
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            // 2. If priority was sent, filter by the priority level of the incident
            if (priority != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), priority));
            }

            // If status was sent, filter by the current status of the incident
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            // 3. If userId was sent, filter by the user who reported the incident
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }

            // 5. If startDate was sent, keep incidents created on or after that date
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            // 6. If endDate was sent, keep incidents created up to that date
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            // Order by creation date, descending (newest first)
            if (query != null) {
                query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            }

            // Combine every active filter with a logical AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
