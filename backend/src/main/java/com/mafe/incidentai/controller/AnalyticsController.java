package com.mafe.incidentai.controller;

import com.mafe.incidentai.domain.enums.Priority;
import com.mafe.incidentai.dto.AnalyticsDashboardDTO;
import com.mafe.incidentai.dto.CategoryMetricDTO;
import com.mafe.incidentai.dto.WeeklyTrendDTO;
import com.mafe.incidentai.service.AnalyticsService;
import com.mafe.incidentai.service.AnalyticsService.Filter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
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
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // 1. Devuelve el resumen consolidado de todos los indicadores analiticos para el dashboard
    @Operation(
        summary = "Get consolidated analytics dashboard KPIs",
        description = "Calcula en base de datos la tasa de resolucion, tiempo promedio en horas, calificacion promedio de IA, top categorias y tendencias semanales. Filtros opcionales por categoria, prioridad y fechas."
    )
    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsDashboardDTO> getDashboardMetrics(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(analyticsService.getDashboardMetrics(new Filter(categoryId, priority, startDate, endDate)));
    }

    // 2. Devuelve el ranking de categorias tecnicas ordenadas por volumen de incidentes
    @Operation(
        summary = "Get most frequent incident categories",
        description = "Retorna las categorias ordenadas de mayor a menor cantidad de incidentes junto con su porcentaje sobre el total filtrado."
    )
    @GetMapping("/top-categories")
    public ResponseEntity<List<CategoryMetricDTO>> getTopCategories(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(analyticsService.getTopCategories(new Filter(categoryId, priority, startDate, endDate)));
    }

    // 3. Devuelve la evolucion semanal de incidentes reportados vs resueltos
    @Operation(
        summary = "Get weekly incident trends",
        description = "Agrupa los incidentes por semana ISO (Ano-Semana) mostrando el volumen de incidentes reportados y resueltos."
    )
    @GetMapping("/weekly-trends")
    public ResponseEntity<List<WeeklyTrendDTO>> getWeeklyTrends(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(analyticsService.getWeeklyTrends(new Filter(categoryId, priority, startDate, endDate)));
    }
}
