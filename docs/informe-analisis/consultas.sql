-- Consultas SQL del informe de análisis de datos.
-- Compatibles con PostgreSQL y con H2 en modo PostgreSQL (base de desarrollo).
-- "Resuelto" = estado RESOLVED o CLOSED, igual que en AnalyticsService.
-- Tiempo de atención = updated_at - created_at de los incidentes resueltos.

-- Q1. Indicadores generales
SELECT COUNT(*)                                                             AS total_incidentes,
       SUM(CASE WHEN status IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END)    AS resueltos,
       SUM(CASE WHEN status NOT IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END) AS pendientes,
       ROUND(100.0 * SUM(CASE WHEN status IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END) / COUNT(*), 2) AS tasa_resolucion_pct,
       ROUND(AVG(CASE WHEN status IN ('RESOLVED', 'CLOSED')
                      THEN EXTRACT(EPOCH FROM (updated_at - created_at)) / 3600.0 END), 2) AS horas_promedio_atencion
FROM incidents;

-- Q2. Incidentes por categoría: volumen, participación, resolución y tiempo
SELECT c.name                                                                AS categoria,
       COUNT(i.id)                                                           AS incidentes,
       ROUND(100.0 * COUNT(i.id) / (SELECT COUNT(*) FROM incidents), 2)      AS participacion_pct,
       ROUND(100.0 * SUM(CASE WHEN i.status IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END) / COUNT(i.id), 2) AS tasa_resolucion_pct,
       ROUND(AVG(CASE WHEN i.status IN ('RESOLVED', 'CLOSED')
                      THEN EXTRACT(EPOCH FROM (i.updated_at - i.created_at)) / 3600.0 END), 2) AS horas_promedio
FROM categories c
JOIN incidents i ON i.category_id = c.id
GROUP BY c.name
ORDER BY incidentes DESC;

-- Q3. Incidentes por prioridad: volumen, pendientes y tiempo de atención
SELECT i.priority                                                            AS prioridad,
       COUNT(*)                                                              AS incidentes,
       SUM(CASE WHEN i.status NOT IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END) AS pendientes,
       ROUND(AVG(CASE WHEN i.status IN ('RESOLVED', 'CLOSED')
                      THEN EXTRACT(EPOCH FROM (i.updated_at - i.created_at)) / 3600.0 END), 2) AS horas_promedio
FROM incidents i
GROUP BY i.priority
ORDER BY CASE i.priority WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END;

-- Q4. Distribución por estado
SELECT status AS estado, COUNT(*) AS incidentes
FROM incidents
GROUP BY status
ORDER BY incidentes DESC;

-- Q5. Tendencia mensual: registrados vs resueltos
SELECT EXTRACT(YEAR FROM created_at)  AS anio,
       EXTRACT(MONTH FROM created_at) AS mes,
       COUNT(*)                       AS registrados,
       SUM(CASE WHEN status IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END) AS resueltos
FROM incidents
GROUP BY EXTRACT(YEAR FROM created_at), EXTRACT(MONTH FROM created_at)
ORDER BY anio, mes;

-- Q6. Tendencia semanal (semana ISO)
SELECT EXTRACT(YEAR FROM created_at) AS anio,
       EXTRACT(WEEK FROM created_at) AS semana,
       COUNT(*)                      AS registrados,
       SUM(CASE WHEN status IN ('RESOLVED', 'CLOSED') THEN 1 ELSE 0 END) AS resueltos
FROM incidents
GROUP BY EXTRACT(YEAR FROM created_at), EXTRACT(WEEK FROM created_at)
ORDER BY anio, semana;

-- Q7. Distribución de tiempos de resolución
SELECT rango, COUNT(*) AS incidentes
FROM (
    SELECT CASE
             WHEN h < 1  THEN '1. < 1 h'
             WHEN h < 4  THEN '2. 1-4 h'
             WHEN h < 8  THEN '3. 4-8 h'
             WHEN h < 24 THEN '4. 8-24 h'
             WHEN h < 72 THEN '5. 1-3 d'
             ELSE '6. > 3 d'
           END AS rango
    FROM (
        SELECT EXTRACT(EPOCH FROM (updated_at - created_at)) / 3600.0 AS h
        FROM incidents
        WHERE status IN ('RESOLVED', 'CLOSED') AND updated_at IS NOT NULL
    ) t
) r
GROUP BY rango
ORDER BY rango;

-- Q8. Desempeño del asistente IA
SELECT q.query_status                         AS estado_consulta,
       COUNT(*)                               AS consultas,
       ROUND(AVG(r.confidence_score), 3)      AS confianza_promedio,
       ROUND(AVG(r.processing_time_ms) / 1000.0, 2) AS segundos_promedio
FROM ai_queries q
LEFT JOIN ai_responses r ON r.ai_query_id = q.id
GROUP BY q.query_status
ORDER BY consultas DESC;

-- Q9. Calificación de las respuestas de la IA
SELECT COUNT(*)                                                         AS calificaciones,
       ROUND(AVG(score), 2)                                             AS promedio_estrellas,
       ROUND(100.0 * SUM(CASE WHEN is_helpful THEN 1 ELSE 0 END) / COUNT(*), 2) AS utiles_pct,
       MIN(score)                                                       AS minima,
       MAX(score)                                                       AS maxima
FROM ratings;

-- Q10. Similitud de las fuentes citadas por la IA
SELECT COUNT(*)                         AS fuentes_citadas,
       ROUND(AVG(similarity_score), 3)  AS similitud_promedio,
       ROUND(MIN(similarity_score), 3)  AS similitud_minima,
       ROUND(MAX(similarity_score), 3)  AS similitud_maxima
FROM response_sources;

-- Q11. Cobertura de la base de conocimiento frente a la demanda por categoría
SELECT c.name                                                              AS categoria,
       (SELECT COUNT(*) FROM incidents i WHERE i.category_id = c.id)       AS incidentes,
       (SELECT COUNT(*) FROM knowledge_articles k
         WHERE k.category_id = c.id AND k.is_active AND k.is_verified)     AS articulos_utilizables,
       ROUND((SELECT COUNT(*) FROM incidents i WHERE i.category_id = c.id) * 1.0
             / NULLIF((SELECT COUNT(*) FROM knowledge_articles k
                        WHERE k.category_id = c.id AND k.is_active AND k.is_verified), 0), 1) AS incidentes_por_articulo
FROM categories c
ORDER BY incidentes DESC;

-- Q12. Problemas recurrentes (títulos repetidos sin el código INC)
SELECT TRIM(REGEXP_REPLACE(title, '\[INC-[0-9]+\]', '')) AS problema,
       COUNT(*)                                          AS ocurrencias
FROM incidents
GROUP BY TRIM(REGEXP_REPLACE(title, '\[INC-[0-9]+\]', ''))
HAVING COUNT(*) > 1
ORDER BY ocurrencias DESC, problema
LIMIT 10;
