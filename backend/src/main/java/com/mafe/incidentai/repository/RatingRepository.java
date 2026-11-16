package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.Rating;
import com.mafe.incidentai.domain.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

// Repository to read the usefulness ratings of AI responses
public interface RatingRepository extends JpaRepository<Rating, Long> {

    Optional<Rating> findByAiResponseId(Long aiResponseId);

    // Computes the real average rating (1 to 5) given to AI responses
    @Query("SELECT COALESCE(AVG(r.score), 0.0) FROM Rating r")
    Double calculateAverageRatingScore();

    // Average AI rating over the incidents that match the analytics filters
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
