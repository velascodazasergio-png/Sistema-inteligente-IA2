package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.AiQuery;
import com.mafe.incidentai.domain.enums.QueryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Repository that tracks the queries sent to n8n
public interface AiQueryRepository extends JpaRepository<AiQuery, Long> {
    List<AiQuery> findByIncidentIdOrderByQueriedAtDesc(Long incidentId);
    List<AiQuery> findByQueryStatus(QueryStatus queryStatus);

    // Global query history, newest first
    List<AiQuery> findAllByOrderByQueriedAtDesc();

    List<AiQuery> findByQueryStatusOrderByQueriedAtDesc(QueryStatus queryStatus);
}
