package com.mafe.incidentai.service;

import com.mafe.incidentai.dto.AnalyticsDashboardDTO;
import com.mafe.incidentai.dto.AnalyticsFilterRequest;
import com.mafe.incidentai.dto.CategoryMetricDTO;
import com.mafe.incidentai.dto.WeeklyTrendDTO;
import com.mafe.incidentai.exception.BusinessRuleException;
import com.mafe.incidentai.repository.IncidentRepository;
import com.mafe.incidentai.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Servicio analitico (RF06) que extrae KPIs reales desde la base de datos mediante consultas JPQL.
// Todos los indicadores aceptan los mismos filtros opcionales: categoria, prioridad y rango de fechas.
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final IncidentRepository incidentRepository;
    private final RatingRepository ratingRepository;

    // Regla de negocio: el rango de fechas debe ser coherente
    private void validate(AnalyticsFilterRequest f) {
        if (f.startDate() != null && f.endDate() != null && f.startDate().isAfter(f.endDate())) {
            throw new BusinessRuleException("Start date cannot be after end date");
        }
    }

    // Consolida todos los indicadores analiticos del dashboard ejecutando las consultas agregadas en BD
    @Transactional(readOnly = true)
    public AnalyticsDashboardDTO getDashboardMetrics(AnalyticsFilterRequest f) {
        validate(f);
        long totalIncidents = incidentRepository.countFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate());
        long resolvedIncidents = incidentRepository.countResolvedOrClosedFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate());

        // Calculo de la tasa de resolucion sobre el total de incidentes filtrados
        double resolutionRate = totalIncidents > 0
                ? roundToTwoDecimals((resolvedIncidents * 100.0) / totalIncidents)
                : 0.0;

        Double avgSeconds = incidentRepository.averageResolutionSecondsFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate());
        double avgResolutionHours = avgSeconds != null ? roundToTwoDecimals(avgSeconds / 3600.0) : 0.0;

        Double rawAvgRating = ratingRepository.averageScoreFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate());
        double avgRatingScore = rawAvgRating != null ? roundToTwoDecimals(rawAvgRating) : 0.0;

        return new AnalyticsDashboardDTO(
                totalIncidents,
                resolvedIncidents,
                resolutionRate,
                avgResolutionHours,
                avgRatingScore,
                getTopCategories(f, totalIncidents),
                getWeeklyTrends(f)
        );
    }

    // Ranking de categorias con mayor volumen de incidentes y su peso porcentual sobre el total filtrado
    @Transactional(readOnly = true)
    public List<CategoryMetricDTO> getTopCategories(AnalyticsFilterRequest f) {
        validate(f);
        return getTopCategories(f, incidentRepository.countFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate()));
    }

    private List<CategoryMetricDTO> getTopCategories(AnalyticsFilterRequest f, long totalIncidents) {
        return incidentRepository.findTopCategoriesFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate())
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

    // Serie de incidentes reportados vs resueltos agrupados por semana ISO, con etiqueta "AAAA-WNN"
    @Transactional(readOnly = true)
    public List<WeeklyTrendDTO> getWeeklyTrends(AnalyticsFilterRequest f) {
        validate(f);
        return incidentRepository.findWeeklyTrendsFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate())
                .stream()
                .map(row -> new WeeklyTrendDTO(
                        String.format("%d-W%02d", ((Number) row[0]).intValue(), ((Number) row[1]).intValue()),
                        ((Number) row[2]).longValue(),
                        row[3] != null ? ((Number) row[3]).longValue() : 0L
                ))
                .toList();
    }

    // Redondea valores decimales a 2 cifras para presentacion limpia en el dashboard
    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
