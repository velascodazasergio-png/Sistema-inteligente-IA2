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

// Repositorio de incidentes con soporte para especificaciones dinamicas y las 10 consultas analiticas de RF06
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findByCategoryIdAndPriority(Long categoryId, Priority priority);
    List<Incident> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Filtros opcionales compartidos por las consultas analiticas: un parametro nulo no filtra
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

    // 1. Total de incidentes registrados que cumplen los filtros
    @Query("SELECT COUNT(i) FROM Incident i WHERE " + ANALYTICS_FILTERS)
    long countFiltered(@Param("categoryId") Long categoryId,
                       @Param("priority") Priority priority,
                       @Param("startDate") LocalDateTime startDate,
                       @Param("endDate") LocalDateTime endDate);

    // 5. Incidentes resueltos o cerrados, base del porcentaje de incidentes resueltos
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status IN " + RESOLVED_STATUSES + " AND " + ANALYTICS_FILTERS)
    long countResolvedOrClosedFiltered(@Param("categoryId") Long categoryId,
                                       @Param("priority") Priority priority,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // 10a. Incidentes pendientes (en estado OPEN, IN_AI_ANALYSIS o ASSIGNED_TO_TECH)
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status IN " + PENDING_STATUSES + " AND " + ANALYTICS_FILTERS)
    long countPendingFiltered(@Param("categoryId") Long categoryId,
                              @Param("priority") Priority priority,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);

    // 10b. Incidentes vencidos (pendientes cuya fecha de creacion es anterior al limite SLA)
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status IN " + PENDING_STATUSES
            + " AND i.createdAt < :overdueThreshold AND " + ANALYTICS_FILTERS)
    long countOverdueFiltered(@Param("categoryId") Long categoryId,
                              @Param("priority") Priority priority,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate,
                              @Param("overdueThreshold") LocalDateTime overdueThreshold);

    // 6. Tiempo promedio de resolucion en segundos (updatedAt - createdAt) de los incidentes resueltos o cerrados
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

    // 2 y 7. Incidentes por categoria ordenados de mayor a menor frecuencia de problemas
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

    // 3. Incidentes agrupados por nivel de prioridad
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

    // 4. Incidentes agrupados por estado actual
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

    // 8. Evolucion semanal: incidentes reportados y resueltos por anio y semana ISO
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

    // 9. Evolucion mensual: incidentes reportados y resueltos agrupados por anio y mes
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
