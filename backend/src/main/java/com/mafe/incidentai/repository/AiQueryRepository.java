package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.AiQuery;
import com.mafe.incidentai.domain.enums.QueryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repositorio para trazabilidad de las consultas enviadas a n8n
public interface AiQueryRepository extends JpaRepository<AiQuery, Long> {
    List<AiQuery> findByIncidentIdOrderByQueriedAtDesc(Long incidentId);
    List<AiQuery> findByQueryStatus(QueryStatus queryStatus);

    // Historial global de consultas, de la mas reciente a la mas antigua
    List<AiQuery> findAllByOrderByQueriedAtDesc();

    List<AiQuery> findByQueryStatusOrderByQueriedAtDesc(QueryStatus queryStatus);
}
