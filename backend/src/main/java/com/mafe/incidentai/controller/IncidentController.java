package com.mafe.incidentai.controller;

import com.mafe.incidentai.domain.enums.Priority;
import com.mafe.incidentai.dto.CreateIncidentRequestDTO;
import com.mafe.incidentai.dto.IncidentResponseDTO;
import com.mafe.incidentai.dto.UpdateIncidentStatusRequestDTO;
import com.mafe.incidentai.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

// Controlador REST para registrar, actualizar estados y consultar incidentes con filtros dinamicos
@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents Management", description = "Endpoints para crear, actualizar estado y filtrar incidentes tecnologicos")
public class IncidentController {

    private final IncidentService incidentService;

    // Registra un nuevo incidente validando el DTO de entrada con @Valid
    @Operation(summary = "Register a new technological incident")
    @PostMapping
    public ResponseEntity<IncidentResponseDTO> createIncident(
            @Valid @RequestBody CreateIncidentRequestDTO request) {
        IncidentResponseDTO response = incidentService.createIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Actualiza el estado de un incidente existente
    @Operation(summary = "Update the status of an existing incident")
    @PatchMapping("/{incidentId}/status")
    public ResponseEntity<IncidentResponseDTO> updateIncidentStatus(
            @PathVariable Long incidentId,
            @Valid @RequestBody UpdateIncidentStatusRequestDTO request) {
        IncidentResponseDTO response = incidentService.updateIncidentStatus(incidentId, request.status());
        return ResponseEntity.ok(response);
    }

    // Consulta incidentes aplicando filtros opcionales por categoria, prioridad, usuario y fechas
    @Operation(summary = "Filter incidents dynamically by category, priority, user, and date range")
    @GetMapping
    public ResponseEntity<List<IncidentResponseDTO>> filterIncidents(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<IncidentResponseDTO> results = incidentService.filterIncidents(
                categoryId, priority, userId, startDate, endDate
        );
        return ResponseEntity.ok(results);
    }
}

