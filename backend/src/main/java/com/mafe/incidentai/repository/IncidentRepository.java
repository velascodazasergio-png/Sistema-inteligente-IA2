package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.Incident;
import com.mafe.incidentai.domain.enums.IncidentStatus;
import com.mafe.incidentai.domain.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

// Repositorio de incidentes que extiende JpaSpecificationExecutor para permitir filtrado dinamico con Criteria API
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {
    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findByCategoryIdAndPriority(Long categoryId, Priority priority);
    List<Incident> findByUserIdOrderByCreatedAtDesc(Long userId);
}
