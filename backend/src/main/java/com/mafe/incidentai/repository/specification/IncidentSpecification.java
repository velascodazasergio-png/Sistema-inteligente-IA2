package com.mafe.incidentai.repository.specification;

import com.mafe.incidentai.domain.entity.Incident;
import com.mafe.incidentai.domain.enums.Priority;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Clase utilitaria que construye predicados JPA dinamicos segun los filtros que envie el usuario
public final class IncidentSpecification {

    private IncidentSpecification() {
        // Constructor privado para evitar instanciacion de clase utilitaria
    }

    public static Specification<Incident> withDynamicFilters(
            Long categoryId,
            Priority priority,
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Si se envio categoryId, filtramos por la llave foranea de categoria
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            // 2. Si se envio priority, filtramos por el nivel de prioridad del incidente
            if (priority != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), priority));
            }

            // 3. Si se envio userId, filtramos por el usuario que reporto el incidente
            if (userId != null) {
                predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));
            }

            // 4. Si se envio startDate, filtramos incidentes creados desde esa fecha en adelante
            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            // 5. Si se envio endDate, filtramos incidentes creados hasta esa fecha limite
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            // Ordenamos por fecha de creacion descendente (los mas recientes primero)
            if (query != null) {
                query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            }

            // Combinamos todos los filtros activos mediante el operador logico AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

