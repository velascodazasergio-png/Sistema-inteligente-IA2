package com.mafe.incidentai.service;

import com.mafe.incidentai.domain.enums.IncidentStatus;
import com.mafe.incidentai.domain.enums.Priority;
import com.mafe.incidentai.dto.*;
import com.mafe.incidentai.exception.BusinessRuleException;
import com.mafe.incidentai.repository.IncidentRepository;
import com.mafe.incidentai.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Analytics service (RF06) that computes the 10 real KPIs in the database with JPQL queries.
// Every indicator accepts the same optional filters: category, priority and date range.
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    // Default SLA threshold: a pending incident unresolved for more than 48 hours is considered overdue
    private static final long SLA_OVERDUE_HOURS = 48L;

    private final IncidentRepository incidentRepository;
    private final RatingRepository ratingRepository;

    // Business rule: the date range must be consistent
    private void validate(AnalyticsFilterRequest f) {
        if (f.startDate() != null && f.endDate() != null && f.startDate().isAfter(f.endDate())) {
            throw new BusinessRuleException("Start date cannot be after end date");
        }
    }

    // Consolidates the 10 dashboard indicators by running the aggregate queries in the database
    @Transactional(readOnly = true)
    public AnalyticsDashboardDTO getDashboardMetrics(AnalyticsFilterRequest f) {
        validate(f);
        long totalIncidents = incidentRepository.countFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate());
        long resolvedIncidents = incidentRepository.countResolvedOrClosedFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate());
        long pendingIncidents = incidentRepository.countPendingFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate());

        LocalDateTime overdueThreshold = LocalDateTime.now().minusHours(SLA_OVERDUE_HOURS);
        long overdueIncidents = incidentRepository.countOverdueFiltered(
                f.categoryId(), f.priority(), f.startDate(), f.endDate(), overdueThreshold
        );

        // Resolution rate over the total of filtered incidents
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
                pendingIncidents,
                overdueIncidents,
                resolutionRate,
                avgResolutionHours,
                avgRatingScore,
                getTopCategories(f, totalIncidents),
                getIncidentsByPriority(f, totalIncidents),
                getIncidentsByStatus(f, totalIncidents),
                getWeeklyTrends(f),
                getMonthlyTrends(f)
        );
    }

    // 2 and 7. Ranking of the categories with the most incidents and their percentage
    @Transactional(readOnly = true)
    public List<CategoryMetricDTO> getTopCategories(AnalyticsFilterRequest f) {
        validate(f);
        return getTopCategories(f, incidentRepository.countFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate()));
    }

    // Incidents per category with their percentage of the total
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

    // 3. Distribution of incidents grouped by priority
    @Transactional(readOnly = true)
    public List<PriorityMetricDTO> getIncidentsByPriority(AnalyticsFilterRequest f) {
        validate(f);
        return getIncidentsByPriority(f, incidentRepository.countFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate()));
    }

    // Incidents per priority with their percentage of the total
    private List<PriorityMetricDTO> getIncidentsByPriority(AnalyticsFilterRequest f, long totalIncidents) {
        return incidentRepository.findIncidentsByPriorityFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate())
                .stream()
                .map(row -> {
                    Priority priority = (Priority) row[0];
                    Long count = ((Number) row[1]).longValue();
                    double percentage = totalIncidents > 0
                            ? roundToTwoDecimals((count * 100.0) / totalIncidents)
                            : 0.0;
                    return new PriorityMetricDTO(priority, count, percentage);
                })
                .toList();
    }

    // 4. Distribution of incidents grouped by operational status
    @Transactional(readOnly = true)
    public List<StatusMetricDTO> getIncidentsByStatus(AnalyticsFilterRequest f) {
        validate(f);
        return getIncidentsByStatus(f, incidentRepository.countFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate()));
    }

    // Incidents per status with their percentage of the total
    private List<StatusMetricDTO> getIncidentsByStatus(AnalyticsFilterRequest f, long totalIncidents) {
        return incidentRepository.findIncidentsByStatusFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate())
                .stream()
                .map(row -> {
                    IncidentStatus status = (IncidentStatus) row[0];
                    Long count = ((Number) row[1]).longValue();
                    double percentage = totalIncidents > 0
                            ? roundToTwoDecimals((count * 100.0) / totalIncidents)
                            : 0.0;
                    return new StatusMetricDTO(status, count, percentage);
                })
                .toList();
    }

    // 8. Series of reported vs resolved incidents grouped by ISO week ("YYYY-WNN")
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

    // 9. Series of reported vs resolved incidents grouped by month ("YYYY-MM")
    @Transactional(readOnly = true)
    public List<MonthlyTrendDTO> getMonthlyTrends(AnalyticsFilterRequest f) {
        validate(f);
        return incidentRepository.findMonthlyTrendsFiltered(f.categoryId(), f.priority(), f.startDate(), f.endDate())
                .stream()
                .map(row -> new MonthlyTrendDTO(
                        String.format("%d-%02d", ((Number) row[0]).intValue(), ((Number) row[1]).intValue()),
                        ((Number) row[2]).longValue(),
                        row[3] != null ? ((Number) row[3]).longValue() : 0L
                ))
                .toList();
    }

    // Rounds decimals to 2 places for a clean display on the dashboard
    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
