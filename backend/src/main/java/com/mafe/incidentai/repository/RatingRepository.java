package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

// Repositorio para consultar las calificaciones de utilidad sobre las respuestas de la IA
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByAiResponseId(Long aiResponseId);

    // Calcula el promedio real de calificacion (1 a 5) otorgado a las respuestas de la IA
    @Query("SELECT COALESCE(AVG(r.score), 0.0) FROM Rating r")
    Double calculateAverageRatingScore();
}
