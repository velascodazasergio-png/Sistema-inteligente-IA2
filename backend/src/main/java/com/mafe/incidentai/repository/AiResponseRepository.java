package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.AiResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repositorio para gestionar las respuestas generadas por la IA
public interface AiResponseRepository extends JpaRepository<AiResponse, Long> {
    Optional<AiResponse> findByAiQueryId(Long aiQueryId);
}

