package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// Contrato DTO consolidado con los 10 indicadores analiticos exigidos en el modulo RF06
@Schema(description = "Indicadores clave de desempeno (KPIs) extraidos mediante consultas SQL/JPQL para el dashboard (RF06)")
public record AnalyticsDashboardDTO(
        @Schema(description = "1. Total de incidentes registrados", example = "150")
        Long totalIncidents,

        @Schema(description = "Total de incidentes en estado RESOLVED o CLOSED", example = "126")
        Long resolvedIncidents,

        @Schema(description = "10a. Total de incidentes pendientes (OPEN, IN_AI_ANALYSIS, ASSIGNED_TO_TECH)", example = "24")
        Long pendingIncidents,

        @Schema(description = "10b. Total de incidentes pendientes vencidos (> 48 horas sin resolverse)", example = "9")
        Long overdueIncidents,

        @Schema(description = "5. Porcentaje de incidentes resueltos (0 a 100)", example = "84.0")
        Double resolutionRatePercentage,

        @Schema(description = "6. Tiempo promedio de resolucion en horas", example = "4.35")
        Double averageResolutionTimeHours,

        @Schema(description = "Promedio de calificacion de utilidad de las respuestas de la IA (escala 1 a 5)", example = "4.75")
        Double averageAiRatingScore,

        @Schema(description = "2 y 7. Incidentes por categoria ordenados por mayor frecuencia de problemas")
        List<CategoryMetricDTO> topCategories,

        @Schema(description = "3. Distribucion de incidentes por nivel de prioridad")
        List<PriorityMetricDTO> incidentsByPriority,

        @Schema(description = "4. Distribucion de incidentes por estado actual")
        List<StatusMetricDTO> incidentsByStatus,

        @Schema(description = "8. Evolucion semanal de incidentes")
        List<WeeklyTrendDTO> weeklyTrends,

        @Schema(description = "9. Evolucion mensual de incidentes")
        List<MonthlyTrendDTO> monthlyTrends
) {}
