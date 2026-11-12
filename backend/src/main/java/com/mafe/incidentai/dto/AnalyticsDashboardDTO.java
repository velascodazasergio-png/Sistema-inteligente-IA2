package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// Contrato DTO consolidado con todos los indicadores analiticos del dashboard (RF06)
@Schema(description = "Indicadores clave de desempeno (KPIs) extraidos mediante consultas SQL/JPQL para el dashboard")
public record AnalyticsDashboardDTO(
        @Schema(description = "Total de incidentes registrados en la base de datos", example = "150")
        Long totalIncidents,

        @Schema(description = "Total de incidentes en estado RESOLVED o CLOSED", example = "126")
        Long resolvedIncidents,

        @Schema(description = "Tasa de resolucion expresada en porcentaje (0 a 100)", example = "84.0")
        Double resolutionRatePercentage,

        @Schema(description = "Tiempo promedio de atencion y resolucion en horas", example = "4.35")
        Double averageResolutionTimeHours,

        @Schema(description = "Promedio de calificacion de utilidad de las respuestas de la IA (escala 1 a 5)", example = "4.6")
        Double averageAiRatingScore,

        @Schema(description = "Ranking de categorias con mayor volumen de incidentes")
        List<CategoryMetricDTO> topCategories,

        @Schema(description = "Serie historica de tendencias semanales de incidentes")
        List<WeeklyTrendDTO> weeklyTrends,

        @Schema(description = "Serie historica de tendencias mensuales de incidentes")
        List<MonthlyTrendDTO> monthlyTrends
) {}

