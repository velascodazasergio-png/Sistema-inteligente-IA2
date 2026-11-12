package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repositorio para consultar las calificaciones de utilidad sobre las respuestas de la IA
public interface RatingRepository extends JpaRepository<Rating, Long> {
    Optional<Rating> findByAiResponseId(Long aiResponseId);
}

