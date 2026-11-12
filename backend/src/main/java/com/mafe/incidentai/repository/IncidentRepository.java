package com.mafe.incidentai.repository;

import com.mafe.incidentai.domain.entity.Incident;
import com.mafe.incidentai.domain.enums.IncidentStatus;
import com.mafe.incidentai.domain.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// Repositorio de incidentes con soporte para especificaciones dinamicas y consultas analiticas (RF06)
public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findByCategoryIdAndPriority(Long categoryId, Priority priority);
    List<Incident> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 1. Cuenta el total de incidentes resueltos o cerrados mediante JPQL para calcular la tasa de resolucion
    @Query("""
            SELECT COUNT(i)
            FROM Incident i
            WHERE i.status IN (com.mafe.incidentai.domain.enums.IncidentStatus.RESOLVED,
                               com.mafe.incidentai.domain.enums.IncidentStatus.CLOSED)
            """)
    long countResolvedOrClosedIncidents();

    // 2. Calcula el tiempo promedio de atencion en horas (diferencia entre updated_at y created_at) en PostgreSQL
    @Query(value = """
            SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (i.updated_at - i.created_at)) / 3600.0), 0.0)
            FROM incidents i
            WHERE i.status IN ('RESOLVED', 'CLOSED')
              AND i.updated_at IS NOT NULL
            """, nativeQuery = true)
    Double calculateAverageResolutionTimeHours();

    // 3. Agrupa los incidentes por categoria y los ordena de mayor a menor frecuencia (Top Categorias)
    @Query(value = """
            SELECT c.id AS categoryId,
                   c.name AS categoryName,
                   COUNT(i.id) AS incidentCount
            FROM categories c
            LEFT JOIN incidents i ON i.category_id = c.id
            GROUP BY c.id, c.name
            ORDER BY incidentCount DESC, c.name ASC
            """, nativeQuery = true)
    List<Object[]> findTopCategoriesRaw();

    // 4. Obtiene la tendencia semanal agrupando por Ano-Semana ISO en PostgreSQL
    @Query(value = """
            SELECT w.yearWeek,
                   COUNT(*) AS totalReported,
                   SUM(CASE WHEN w.status IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END) AS totalResolved
            FROM (
                SELECT TO_CHAR(DATE_TRUNC('week', i.created_at), 'YYYY-"W"IW') AS yearWeek,
                       DATE_TRUNC('week', i.created_at) AS weekStart,
                       i.status AS status
                FROM incidents i
            ) w
            GROUP BY w.yearWeek, w.weekStart
            ORDER BY w.weekStart ASC
            """, nativeQuery = true)
    List<Object[]> findWeeklyTrendsRaw();
}
