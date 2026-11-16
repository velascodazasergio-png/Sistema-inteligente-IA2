package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.Incident;
import com.mafe.incidentai.domain.enums.IncidentStatus;
import com.mafe.incidentai.domain.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

// Incident repository with support for dynamic specifications and the 10 RF06 analytics queries
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    // Incidents in a given status
    List<Incident> findByStatus(IncidentStatus status);
    // Incidents of a category with a given priority
    List<Incident> findByCategoryIdAndPriority(Long categoryId, Priority priority);
    // Incidents reported by a user, newest first
    List<Incident> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Optional filters shared by the analytics queries: a null parameter does not filter
    String ANALYTICS_FILTERS = """
             (:categoryId IS NULL OR i.category.id = :categoryId)
             AND (:priority IS NULL OR i.priority = :priority)
             AND (CAST(:startDate AS LocalDateTime) IS NULL OR i.createdAt >= :startDate)
             AND (CAST(:endDate AS LocalDateTime) IS NULL OR i.createdAt <= :endDate)
            """;

    String RESOLVED_STATUSES = """
            (com.mafe.incidentai.domain.enums.IncidentStatus.RESOLVED,
             com.mafe.incidentai.domain.enums.IncidentStatus.CLOSED)
            """;

    String PENDING_STATUSES = """
            (com.mafe.incidentai.domain.enums.IncidentStatus.OPEN,
             com.mafe.incidentai.domain.enums.IncidentStatus.IN_AI_ANALYSIS,
             com.mafe.incidentai.domain.enums.IncidentStatus.ASSIGNED_TO_TECH)
            """;

    // 1. Total registered incidents that match the filters
    @Query("SELECT COUNT(i) FROM Incident i WHERE " + ANALYTICS_FILTERS)
    long countFiltered(@Param("categoryId") Long categoryId,
                       @Param("priority") Priority priority,
                       @Param("startDate") LocalDateTime startDate,
                       @Param("endDate") LocalDateTime endDate);

    // 5. Resolved or closed incidents, the base of the resolution percentage
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status IN " + RESOLVED_STATUSES + " AND " + ANALYTICS_FILTERS)
    long countResolvedOrClosedFiltered(@Param("categoryId") Long categoryId,
                                       @Param("priority") Priority priority,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // 10a. Pending incidents (status OPEN, IN_AI_ANALYSIS or ASSIGNED_TO_TECH)
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status IN " + PENDING_STATUSES + " AND " + ANALYTICS_FILTERS)
    long countPendingFiltered(@Param("categoryId") Long categoryId,
                              @Param("priority") Priority priority,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);

    // 10b. Overdue incidents (pending ones created before the SLA limit)
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status IN " + PENDING_STATUSES
            + " AND i.createdAt < :overdueThreshold AND " + ANALYTICS_FILTERS)
    long countOverdueFiltered(@Param("categoryId") Long categoryId,
                              @Param("priority") Priority priority,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate,
                              @Param("overdueThreshold") LocalDateTime overdueThreshold);

    // 6. Average resolution time in seconds (updatedAt - createdAt) of resolved or closed incidents
    @Query("""
            SELECT AVG((i.updatedAt - i.createdAt) BY SECOND)
            FROM Incident i
            WHERE i.updatedAt IS NOT NULL
              AND i.status IN
            """ + RESOLVED_STATUSES + " AND " + ANALYTICS_FILTERS)
    Double averageResolutionSecondsFiltered(@Param("categoryId") Long categoryId,
                                            @Param("priority") Priority priority,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    // 2 and 7. Incidents per category, from most to least frequent
    @Query("""
            SELECT i.category.id, i.category.name, COUNT(i)
            FROM Incident i
            WHERE
            """ + ANALYTICS_FILTERS + """
            GROUP BY i.category.id, i.category.name
            ORDER BY COUNT(i) DESC, i.category.name ASC
            """)
    List<Object[]> findTopCategoriesFiltered(@Param("categoryId") Long categoryId,
                                             @Param("priority") Priority priority,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    // 3. Incidents grouped by priority level
    @Query("""
            SELECT i.priority, COUNT(i)
            FROM Incident i
            WHERE
            """ + ANALYTICS_FILTERS + """
            GROUP BY i.priority
            ORDER BY COUNT(i) DESC
            """)
    List<Object[]> findIncidentsByPriorityFiltered(@Param("categoryId") Long categoryId,
                                                   @Param("priority") Priority priority,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    // 4. Incidents grouped by current status
    @Query("""
            SELECT i.status, COUNT(i)
            FROM Incident i
            WHERE
            """ + ANALYTICS_FILTERS + """
            GROUP BY i.status
            ORDER BY COUNT(i) DESC
            """)
    List<Object[]> findIncidentsByStatusFiltered(@Param("categoryId") Long categoryId,
                                                 @Param("priority") Priority priority,
                                                 @Param("startDate") LocalDateTime startDate,
                                                 @Param("endDate") LocalDateTime endDate);

    // 8. Weekly trend: reported and resolved incidents per year and ISO week
    @Query("""
            SELECT EXTRACT(YEAR FROM i.createdAt), EXTRACT(WEEK FROM i.createdAt), COUNT(i),
                   SUM(CASE WHEN i.status IN
            """ + RESOLVED_STATUSES + """
                   THEN 1 ELSE 0 END)
            FROM Incident i
            WHERE
            """ + ANALYTICS_FILTERS + """
            GROUP BY EXTRACT(YEAR FROM i.createdAt), EXTRACT(WEEK FROM i.createdAt)
            ORDER BY EXTRACT(YEAR FROM i.createdAt), EXTRACT(WEEK FROM i.createdAt)
            """)
    List<Object[]> findWeeklyTrendsFiltered(@Param("categoryId") Long categoryId,
                                            @Param("priority") Priority priority,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    // 9. Monthly trend: reported and resolved incidents grouped by year and month
    @Query("""
            SELECT EXTRACT(YEAR FROM i.createdAt), EXTRACT(MONTH FROM i.createdAt), COUNT(i),
                   SUM(CASE WHEN i.status IN
            """ + RESOLVED_STATUSES + """
                   THEN 1 ELSE 0 END)
            FROM Incident i
            WHERE
            """ + ANALYTICS_FILTERS + """
            GROUP BY EXTRACT(YEAR FROM i.createdAt), EXTRACT(MONTH FROM i.createdAt)
            ORDER BY EXTRACT(YEAR FROM i.createdAt), EXTRACT(MONTH FROM i.createdAt)
            """)
    List<Object[]> findMonthlyTrendsFiltered(@Param("categoryId") Long categoryId,
                                             @Param("priority") Priority priority,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
}
