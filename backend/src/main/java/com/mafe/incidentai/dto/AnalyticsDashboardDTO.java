package com.mafe.incidentai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// Consolidated DTO with the 10 analytics indicators required by module RF06
@Schema(description = "Key performance indicators (KPIs) computed with SQL/JPQL queries for the dashboard (RF06)")
public record AnalyticsDashboardDTO(
        @Schema(description = "1. Total registered incidents", example = "150")
        Long totalIncidents,

        @Schema(description = "Total incidents in RESOLVED or CLOSED status", example = "126")
        Long resolvedIncidents,

        @Schema(description = "10a. Total pending incidents (OPEN, IN_AI_ANALYSIS, ASSIGNED_TO_TECH)", example = "24")
        Long pendingIncidents,

        @Schema(description = "10b. Total overdue pending incidents (> 48 hours unresolved)", example = "9")
        Long overdueIncidents,

        @Schema(description = "5. Percentage of resolved incidents (0 to 100)", example = "84.0")
        Double resolutionRatePercentage,

        @Schema(description = "6. Average resolution time in hours", example = "4.35")
        Double averageResolutionTimeHours,

        @Schema(description = "Average usefulness rating of the AI responses (1 to 5 scale)", example = "4.75")
        Double averageAiRatingScore,

        @Schema(description = "2 and 7. Incidents per category, most frequent first")
        List<CategoryMetricDTO> topCategories,

        @Schema(description = "3. Distribution of incidents by priority level")
        List<PriorityMetricDTO> incidentsByPriority,

        @Schema(description = "4. Distribution of incidents by current status")
        List<StatusMetricDTO> incidentsByStatus,

        @Schema(description = "8. Weekly incident trend")
        List<WeeklyTrendDTO> weeklyTrends,

        @Schema(description = "9. Monthly incident trend")
        List<MonthlyTrendDTO> monthlyTrends
) {}
