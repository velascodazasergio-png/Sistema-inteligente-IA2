package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.Rating;
import com.mafe.incidentai.domain.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

// Repositorio para consultar las calificaciones de utilidad sobre las respuestas de la IA
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByAiResponseId(Long aiResponseId);

    // Calcula el promedio real de calificacion (1 a 5) otorgado a las respuestas de la IA
    @Query("SELECT COALESCE(AVG(r.score), 0.0) FROM Rating r")
    Double calculateAverageRatingScore();

    // Promedio de calificacion de la IA sobre los incidentes que cumplen los filtros analiticos
    @Query("""
            SELECT AVG(r.score)
            FROM Rating r
            JOIN r.aiResponse ar
            JOIN ar.aiQuery q
            JOIN q.incident i
            WHERE
            """ + IncidentRepository.ANALYTICS_FILTERS)
    Double averageScoreFiltered(@Param("categoryId") Long categoryId,
                                @Param("priority") Priority priority,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);
}
