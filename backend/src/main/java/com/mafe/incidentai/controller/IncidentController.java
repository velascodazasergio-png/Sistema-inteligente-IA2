package com.mafe.incidentai.controller;

import com.mafe.incidentai.security.AccessPolicy;
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

// REST controller to register, read, update, change the status of and filter incidents (RF01)
@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Tag(name = "Incidents Management (RF01)", description = "Endpoints to create, read, update, change the status of and filter technology incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final AccessPolicy accessPolicy;

    // 1. Registers a new incident, validating the input DTO with @Valid
    @Operation(summary = "Register a new technological incident", description = "The incident starts in the OPEN status.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Incident registered"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "User or category not found"),
        @ApiResponse(responseCode = "409", description = "The category is inactive")
    })
    @PostMapping
    public ResponseEntity<IncidentResponseDTO> createIncident(
            @Valid @RequestBody CreateIncidentRequestDTO request) {
        // An employee always reports in their own name, even if they send another userId
        if (accessPolicy.isEmployee()) {
            request = new CreateIncidentRequestDTO(request.title(), request.description(), request.priority(),
                    accessPolicy.user().id(), request.categoryId());
        }
        IncidentResponseDTO response = incidentService.createIncident(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 2. Returns the detail of an incident by its ID
    @Operation(summary = "Get an incident by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Incident detail"),
        @ApiResponse(responseCode = "404", description = "Incident not found")
    })
    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentResponseDTO> getIncident(@PathVariable Long incidentId) {
        IncidentResponseDTO incident = incidentService.getIncident(incidentId);
        accessPolicy.checkIncident(incident);
        return ResponseEntity.ok(incident);
    }

    // 3. Updates the general information of an existing incident (title, description, priority, category, user)
    @Operation(summary = "Update an existing incident's information")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Incident information updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "404", description = "Incident, user or category not found"),
        @ApiResponse(responseCode = "409", description = "The incident is already closed (CLOSED) or the category is inactive")
    })
    @PutMapping("/{incidentId}")
    public ResponseEntity<IncidentResponseDTO> updateIncident(
            @PathVariable Long incidentId,
            @Valid @RequestBody CreateIncidentRequestDTO request) {
        return ResponseEntity.ok(incidentService.updateIncident(incidentId, request));
    }

    // 4. Updates the status of an existing incident
    @Operation(summary = "Update the status of an existing incident")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Status updated"),
        @ApiResponse(responseCode = "400", description = "Invalid or missing status"),
        @ApiResponse(responseCode = "404", description = "Incident not found"),
        @ApiResponse(responseCode = "409", description = "The incident is already closed (CLOSED)")
    })
    @PatchMapping("/{incidentId}/status")
    public ResponseEntity<IncidentResponseDTO> updateIncidentStatus(
            @PathVariable Long incidentId,
            @Valid @RequestBody UpdateIncidentStatusRequestDTO request) {
        IncidentResponseDTO response = incidentService.updateIncidentStatus(incidentId, request.status());
        return ResponseEntity.ok(response);
    }

    // 5. Searches incidents with optional filters by category, priority, status, user and dates
    @Operation(summary = "Filter incidents dynamically by category, priority, status, user, and date range")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Incidents that match the filters, newest first"),
        @ApiResponse(responseCode = "400", description = "Priority, status or date with an invalid format"),
        @ApiResponse(responseCode = "409", description = "The start date is after the end date")
    })
    @GetMapping
    public ResponseEntity<List<IncidentResponseDTO>> filterIncidents(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        // An employee only sees their own incidents
        if (accessPolicy.isEmployee()) userId = accessPolicy.user().id();
        List<IncidentResponseDTO> results = incidentService.filterIncidents(
                categoryId, priority, status, userId, startDate, endDate
        );
        return ResponseEntity.ok(results);
    }
}
