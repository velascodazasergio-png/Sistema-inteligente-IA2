package com.mafe.incidentai.controller;

import com.mafe.incidentai.domain.enums.IncidentStatus;
import com.mafe.incidentai.domain.enums.Priority;
import com.mafe.incidentai.dto.CreateIncidentRequestDTO;
import com.mafe.incidentai.dto.IncidentResponseDTO;
import com.mafe.incidentai.dto.UpdateIncidentStatusRequestDTO;
import com.mafe.incidentai.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

// Controlador REST para registrar, consultar, actualizar informacion, cambiar estados y filtrar incidentes (RF01)
@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents Management (RF01)", description = "Endpoints para crear, consultar, actualizar informacion, cambiar estado y filtrar incidentes tecnologicos")
public class IncidentController {

    private final IncidentService incidentService;

    // 1. Registra un nuevo incidente validando el DTO de entrada con @Valid
    @Operation(summary = "Register a new technological incident", description = "El incidente nace en estado OPEN.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Incidente registrado"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
        @ApiResponse(responseCode = "404", description = "Usuario o categoria no encontrados"),
        @ApiResponse(responseCode = "409", description = "La categoria esta inactiva")
    })
    @PostMapping
    public ResponseEntity<IncidentResponseDTO> createIncident(
            @Valid @RequestBody CreateIncidentRequestDTO request) {
        IncidentResponseDTO response = incidentService.createIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Devuelve el detalle de un incidente por su identificador
    @Operation(summary = "Get an incident by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Detalle del incidente"),
        @ApiResponse(responseCode = "404", description = "Incidente no encontrado")
    })
    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentResponseDTO> getIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(incidentService.getIncident(incidentId));
    }

    // 3. Actualiza la informacion general de un incidente existente (titulo, descripcion, prioridad, categoria, usuario)
    @Operation(summary = "Update an existing incident's information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Informacion del incidente actualizada"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
        @ApiResponse(responseCode = "404", description = "Incidente, usuario o categoria no encontrados"),
        @ApiResponse(responseCode = "409", description = "El incidente ya esta cerrado (CLOSED) o la categoria esta inactiva")
    })
    @PutMapping("/{incidentId}")
    public ResponseEntity<IncidentResponseDTO> updateIncident(
            @PathVariable Long incidentId,
            @Valid @RequestBody CreateIncidentRequestDTO request) {
        return ResponseEntity.ok(incidentService.updateIncident(incidentId, request));
    }

    // 4. Actualiza el estado de un incidente existente
    @Operation(summary = "Update the status of an existing incident")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado actualizado"),
        @ApiResponse(responseCode = "400", description = "Estado invalido o ausente"),
        @ApiResponse(responseCode = "404", description = "Incidente no encontrado"),
        @ApiResponse(responseCode = "409", description = "El incidente ya esta cerrado (CLOSED)")
    })
    @PatchMapping("/{incidentId}/status")
    public ResponseEntity<IncidentResponseDTO> updateIncidentStatus(
            @PathVariable Long incidentId,
            @Valid @RequestBody UpdateIncidentStatusRequestDTO request) {
        IncidentResponseDTO response = incidentService.updateIncidentStatus(incidentId, request.status());
        return ResponseEntity.ok(response);
    }

    // 5. Consulta incidentes aplicando filtros opcionales por categoria, prioridad, estado, usuario y fechas
    @Operation(summary = "Filter incidents dynamically by category, priority, status, user, and date range")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Incidentes que cumplen los filtros, del mas reciente al mas antiguo"),
        @ApiResponse(responseCode = "400", description = "Prioridad, estado o fecha con formato invalido"),
        @ApiResponse(responseCode = "409", description = "La fecha inicial es posterior a la final")
    })
    @GetMapping
    public ResponseEntity<List<IncidentResponseDTO>> filterIncidents(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<IncidentResponseDTO> results = incidentService.filterIncidents(
                categoryId, priority, status, userId, startDate, endDate
        );
        return ResponseEntity.ok(results);
    }
}
