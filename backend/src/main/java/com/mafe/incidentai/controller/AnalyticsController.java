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

// Controlador REST para exponer los 10 indicadores analiticos (RF06) al dashboard del frontend.
// Todos los endpoints aceptan los filtros opcionales categoryId, priority, startDate y endDate.
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(
    name = "Analytics & KPIs (RF06)",
    description = "Endpoints para consultar tasa de resolucion, tiempo promedio, distribucion por categoria/prioridad/estado, evolucion semanal/mensual e incidentes pendientes/vencidos"
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Indicadores calculados"),
    @ApiResponse(responseCode = "400", description = "Prioridad o fecha con formato invalido"),
    @ApiResponse(responseCode = "409", description = "La fecha inicial es posterior a la final")
})
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // 1. Devuelve el resumen consolidado con los 10 indicadores analiticos de RF06 para el dashboard
    @Operation(
        summary = "Get consolidated analytics dashboard KPIs (All 10 RF06 metrics)",
        description = "Calcula en base de datos total, pendientes, vencidos, tasa de resolucion, tiempo promedio en horas, calificacion IA, categorias, prioridades, estados y tendencias semanales/mensuales."
    )
    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsDashboardDTO> getDashboardMetrics(@ParameterObject AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getDashboardMetrics(filter));
    }

    // 2. Devuelve el ranking de categorias tecnicas ordenadas por volumen de incidentes
    @Operation(
        summary = "Get most frequent incident categories",
        description = "Retorna las categorias ordenadas de mayor a menor cantidad de incidentes junto con su porcentaje sobre el total filtrado."
    )
    @GetMapping("/top-categories")
    public ResponseEntity<List<CategoryMetricDTO>> getTopCategories(@ParameterObject AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getTopCategories(filter));
    }

    // 3. Devuelve la distribucion de incidentes por prioridad
    @Operation(
        summary = "Get incidents grouped by priority",
        description = "Retorna la cantidad y el porcentaje de incidentes por cada nivel de prioridad (LOW, MEDIUM, HIGH, CRITICAL)."
    )
    @GetMapping("/by-priority")
    public ResponseEntity<List<PriorityMetricDTO>> getIncidentsByPriority(@ParameterObject AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getIncidentsByPriority(filter));
    }

    // 4. Devuelve la distribucion de incidentes por estado
    @Operation(
        summary = "Get incidents grouped by status",
        description = "Retorna la cantidad y el porcentaje de incidentes en cada estado (OPEN, IN_AI_ANALYSIS, ASSIGNED_TO_TECH, RESOLVED, CLOSED)."
    )
    @GetMapping("/by-status")
    public ResponseEntity<List<StatusMetricDTO>> getIncidentsByStatus(@ParameterObject AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getIncidentsByStatus(filter));
    }

    // 5. Devuelve la evolucion semanal de incidentes reportados vs resueltos
    @Operation(
        summary = "Get weekly incident trends",
        description = "Agrupa los incidentes por semana ISO (Ano-Semana) mostrando el volumen de incidentes reportados y resueltos."
    )
    @GetMapping("/weekly-trends")
    public ResponseEntity<List<WeeklyTrendDTO>> getWeeklyTrends(@ParameterObject AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getWeeklyTrends(filter));
    }

    // 6. Devuelve la evolucion mensual de incidentes reportados vs resueltos
    @Operation(
        summary = "Get monthly incident trends",
        description = "Agrupa los incidentes por mes (AAAA-MM) mostrando el volumen de incidentes reportados y resueltos."
    )
    @GetMapping("/monthly-trends")
    public ResponseEntity<List<MonthlyTrendDTO>> getMonthlyTrends(@ParameterObject AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getMonthlyTrends(filter));
    }
}
