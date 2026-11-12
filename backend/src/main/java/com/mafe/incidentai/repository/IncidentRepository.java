package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.Incident;
import com.mafe.incidentai.domain.enums.IncidentStatus;
import com.mafe.incidentai.domain.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repositorio de incidentes con metodos de consulta derivados (Query Methods de Spring Data JPA)
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findByCategoryIdAndPriority(Long categoryId, Priority priority);
    List<Incident> findByUserIdOrderByCreatedAtDesc(Long userId);
}

