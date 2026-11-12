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

// Repositorio de incidentes con soporte para especificaciones dinamicas y consultas analiticas (RF06)
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

    // 1. Total de incidentes que cumplen los filtros
    @Query("SELECT COUNT(i) FROM Incident i WHERE " + ANALYTICS_FILTERS)
    long countFiltered(@Param("categoryId") Long categoryId,
                       @Param("priority") Priority priority,
                       @Param("startDate") LocalDateTime startDate,
                       @Param("endDate") LocalDateTime endDate);

    // 2. Incidentes resueltos o cerrados, base de la tasa de resolucion
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status IN " + RESOLVED_STATUSES + " AND " + ANALYTICS_FILTERS)
    long countResolvedOrClosedFiltered(@Param("categoryId") Long categoryId,
                                       @Param("priority") Priority priority,
                                       @Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // 3. Tiempo promedio de atencion en segundos (updatedAt - createdAt) de los incidentes resueltos o cerrados
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

    // 4. Categorias ordenadas de mayor a menor numero de incidentes
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

    // 5. Tendencia semanal: incidentes reportados y resueltos por anio y semana ISO
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

    // 6. Tendencia mensual: incidentes reportados y resueltos por anio y mes
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
