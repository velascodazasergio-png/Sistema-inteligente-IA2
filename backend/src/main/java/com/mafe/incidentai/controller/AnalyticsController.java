package com.mafe.incidentai.controller;

import com.mafe.incidentai.dto.*;
import com.mafe.incidentai.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// REST controller that exposes the 10 analytics indicators (RF06) to the frontend dashboard.
// Every endpoint accepts the optional filters categoryId, priority, startDate and endDate.
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(
    name = "Analytics & KPIs (RF06)",
    description = "Endpoints for resolution rate, average time, distribution by category/priority/status, weekly/monthly trends and pending/overdue incidents"
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Indicadores calculados"),
    @ApiResponse(responseCode = "400", description = "Priority or date with an invalid format"),
    @ApiResponse(responseCode = "409", description = "The start date is after the end date")
})
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // 1. Returns the consolidated summary with the 10 RF06 analytics indicators for the dashboard
    @Operation(
        summary = "Get consolidated analytics dashboard KPIs (All 10 RF06 metrics)",
        description = "Computes in the database the total, pending, overdue, resolution rate, average time in hours, AI rating, categories, priorities, statuses and weekly/monthly trends."
    )
    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsDashboardDTO> getDashboardMetrics(@ParameterObject AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getDashboardMetrics(filter));
    }

    // 2. Returns the ranking of technical categories ordered by incident volume
    @Operation(
        summary = "Get most frequent incident categories",
        description = "Returns the categories ordered from most to fewest incidents, with their percentage of the filtered total."
    )
    @GetMapping("/top-categories")
    public ResponseEntity<List<CategoryMetricDTO>> getTopCategories(@ParameterObject AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getTopCategories(filter));
    }

    // 3. Returns the distribution of incidents by priority
    @Operation(
        summary = "Get incidents grouped by priority",
        description = "Returns the count and percentage of incidents for each priority level (LOW, MEDIUM, HIGH, CRITICAL)."
    )
    @GetMapping("/by-priority")
    public ResponseEntity<List<PriorityMetricDTO>> getIncidentsByPriority(@ParameterObject AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getIncidentsByPriority(filter));
    }

    // 4. Returns the distribution of incidents by status
    @Operation(
        summary = "Get incidents grouped by status",
        description = "Returns the count and percentage of incidents in each status (OPEN, IN_AI_ANALYSIS, ASSIGNED_TO_TECH, RESOLVED, CLOSED)."
    )
    @GetMapping("/by-status")
    public ResponseEntity<List<StatusMetricDTO>> getIncidentsByStatus(@ParameterObject AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getIncidentsByStatus(filter));
    }

    // 5. Returns the weekly trend of reported vs resolved incidents
    @Operation(
        summary = "Get weekly incident trends",
        description = "Groups incidents by ISO week (Year-Week), showing the number of reported and resolved incidents."
    )
    @GetMapping("/weekly-trends")
    public ResponseEntity<List<WeeklyTrendDTO>> getWeeklyTrends(@ParameterObject AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getWeeklyTrends(filter));
    }

    // 6. Returns the monthly trend of reported vs resolved incidents
    @Operation(
        summary = "Get monthly incident trends",
        description = "Groups incidents by month (YYYY-MM), showing the number of reported and resolved incidents."
    )
    @GetMapping("/monthly-trends")
    public ResponseEntity<List<MonthlyTrendDTO>> getMonthlyTrends(@ParameterObject AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getMonthlyTrends(filter));
    }
}
