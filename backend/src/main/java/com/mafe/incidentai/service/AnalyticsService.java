package com.mafe.incidentai.service;

import com.mafe.incidentai.dto.AnalyticsDashboardDTO;
import com.mafe.incidentai.dto.CategoryMetricDTO;
import com.mafe.incidentai.dto.WeeklyTrendDTO;
import com.mafe.incidentai.repository.IncidentRepository;
import com.mafe.incidentai.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Servicio analitico (RF06) que extrae KPIs reales desde la base de datos mediante consultas SQL/JPQL
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final IncidentRepository incidentRepository;
    private final RatingRepository ratingRepository;

    // Consolida todos los indicadores analiticos del dashboard ejecutando las consultas agregadas en BD
    @Transactional(readOnly = true)
    public AnalyticsDashboardDTO getDashboardMetrics() {
        long totalIncidents = incidentRepository.count();
        long resolvedIncidents = incidentRepository.countResolvedOrClosedIncidents();

        // Calculo de la tasa de resolucion sobre el total real de incidentes en la base de datos
        double resolutionRate = totalIncidents > 0
                ? roundToTwoDecimals((resolvedIncidents * 100.0) / totalIncidents)
                : 0.0;

        Double rawAvgHours = incidentRepository.calculateAverageResolutionTimeHours();
        double avgResolutionHours = rawAvgHours != null ? roundToTwoDecimals(rawAvgHours) : 0.0;

        Double rawAvgRating = ratingRepository.calculateAverageRatingScore();
        double avgRatingScore = rawAvgRating != null ? roundToTwoDecimals(rawAvgRating) : 0.0;

        List<CategoryMetricDTO> topCategories = getTopCategories();
        List<WeeklyTrendDTO> weeklyTrends = getWeeklyTrends();

        return new AnalyticsDashboardDTO(
                totalIncidents,
                resolvedIncidents,
                resolutionRate,
                avgResolutionHours,
                avgRatingScore,
                topCategories,
                weeklyTrends
        );
    }

    // Extrae el ranking de categorias con mayor volumen de incidentes y calcula su peso porcentual
    @Transactional(readOnly = true)
    public List<CategoryMetricDTO> getTopCategories() {
        long totalIncidents = incidentRepository.count();

        return incidentRepository.findTopCategoriesRaw()
                .stream()
                .map(row -> {
                    Long categoryId = ((Number) row[0]).longValue();
                    String categoryName = (String) row[1];
                    Long incidentCount = ((Number) row[2]).longValue();
                    double percentage = totalIncidents > 0
                            ? roundToTwoDecimals((incidentCount * 100.0) / totalIncidents)
                            : 0.0;

                    return new CategoryMetricDTO(categoryId, categoryName, incidentCount, percentage);
                })
                .toList();
    }

    // Extrae la serie historica de incidentes reportados vs resueltos agrupados por semana ISO
    @Transactional(readOnly = true)
    public List<WeeklyTrendDTO> getWeeklyTrends() {
        return incidentRepository.findWeeklyTrendsRaw()
                .stream()
                .map(row -> new WeeklyTrendDTO(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue()
                ))
                .toList();
    }

    // Redondea valores decimales a 2 cifras para presentacion limpia en el dashboard
    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

