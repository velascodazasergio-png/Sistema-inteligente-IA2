package com.mafe.incidentai.controller;

import com.mafe.incidentai.dto.AnalyticsDashboardDTO;
import com.mafe.incidentai.dto.AnalyticsFilterRequest;
import com.mafe.incidentai.dto.CategoryMetricDTO;
import com.mafe.incidentai.dto.WeeklyTrendDTO;
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

// Controlador REST para exponer los indicadores analiticos (RF06) al dashboard del frontend.
// Todos los endpoints aceptan los filtros opcionales categoryId, priority, startDate y endDate.
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(
    name = "Analytics & KPIs (RF06)",
    description = "Endpoints para consultar tasa de resolucion, tiempo promedio de atencion, categorias frecuentes y tendencias semanales"
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Indicadores calculados"),
    @ApiResponse(responseCode = "400", description = "Prioridad o fecha con formato invalido"),
    @ApiResponse(responseCode = "409", description = "La fecha inicial es posterior a la final")
})
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // 1. Devuelve el resumen consolidado de todos los indicadores analiticos para el dashboard
    @Operation(
        summary = "Get consolidated analytics dashboard KPIs",
        description = "Calcula en base de datos la tasa de resolucion, tiempo promedio en horas, calificacion promedio de IA, top categorias y tendencias semanales."
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

    // 3. Devuelve la evolucion semanal de incidentes reportados vs resueltos
    @Operation(
        summary = "Get weekly incident trends",
        description = "Agrupa los incidentes por semana ISO (Ano-Semana) mostrando el volumen de incidentes reportados y resueltos."
    )
    @GetMapping("/weekly-trends")
    public ResponseEntity<List<WeeklyTrendDTO>> getWeeklyTrends(@ParameterObject AnalyticsFilterRequest filter) {
        return ResponseEntity.ok(analyticsService.getWeeklyTrends(filter));
    }
}
