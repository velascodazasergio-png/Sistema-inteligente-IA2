package com.mafe.incidentai.controller;

import com.mafe.incidentai.domain.enums.QueryStatus;
import com.mafe.incidentai.dto.*;
import com.mafe.incidentai.service.AiAssistanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Controlador REST para orquestar el analisis de incidentes con n8n, registrar respuestas, calificar y consultar historial
@RestController
@RequestMapping("/api/v1/ai-assistance")
@RequiredArgsConstructor
@Tag(
    name = "AI Assistance (n8n Integration)",
    description = "Endpoints para disparar analisis hacia n8n, guardar respuestas en cadena, calificar sugerencias y consultar el historial"
)
public class AiAssistanceController {

    private final AiAssistanceService aiAssistanceService;

    // 1. Dispara el analisis de un incidente hacia el webhook de n8n y registra AiQuery (PENDING o COMMUNICATION_ERROR)
    @Operation(
        summary = "Request AI incident analysis (Trigger to n8n)",
        description = "Registra la consulta AiQuery, dispara el cliente HTTP hacia n8n y actualiza el estado si ocurre timeout o error."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Solicitud procesada y registrada en base de datos"),
        @ApiResponse(responseCode = "404", description = "Incidente no encontrado")
    })
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestAiAssistance(
            @Valid @RequestBody AiAssistanceRequestDTO request) {
        Map<String, Object> result = aiAssistanceService.requestAiAssistance(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    // 2. Recibe el callback desde n8n y guarda en cascada AiResponse y sus ResponseSource con similarityScore
    @Operation(
        summary = "Register AI response and sources (Callback webhook from n8n)",
        description = "Guarda en cadena la respuesta de n8n (AiResponse), las fuentes (ResponseSource) con su similitud y marca AiQuery como COMPLETED."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "AiResponse y sus fuentes guardadas en cadena exitosamente"),
        @ApiResponse(responseCode = "404", description = "AiQuery original no encontrada")
    })
    @PostMapping("/responses")
    public ResponseEntity<Map<String, Object>> registerAiResponse(
            @Valid @RequestBody AiResponseDTO responseDto) {
        Map<String, Object> result = aiAssistanceService.registerAiResponse(responseDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // 3. Registra la calificacion (Rating) sobre una respuesta generada por la IA
    @Operation(
        summary = "Rate an AI response",
        description = "Permite a un tecnico o usuario calificar de 1 a 5 la utilidad de una respuesta generada por la IA."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Calificacion registrada exitosamente"),
        @ApiResponse(responseCode = "404", description = "Respuesta IA o Usuario evaluador no encontrado"),
        @ApiResponse(responseCode = "409", description = "La respuesta IA ya habia sido calificada previamente")
    })
    @PostMapping("/responses/{aiResponseId}/ratings")
    public ResponseEntity<RatingResponseDTO> rateAiResponse(
            @PathVariable Long aiResponseId,
            @Valid @RequestBody CreateRatingRequestDTO request) {
        RatingResponseDTO response = aiAssistanceService.rateAiResponse(aiResponseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 5. Consulta el historial global de consultas a la IA
    @Operation(
        summary = "Get global AI query history",
        description = "Devuelve todas las consultas a la IA, de la mas reciente a la mas antigua. Filtro opcional por estado."
    )
    @GetMapping("/history")
    public ResponseEntity<List<AiQueryHistoryResponseDTO>> getAiHistory(
            @RequestParam(required = false) QueryStatus status) {
        return ResponseEntity.ok(aiAssistanceService.getAiHistory(status));
    }

    // 4. Consulta el historial completo de consultas a la IA (AiQuery) para un incidente
    @Operation(
        summary = "Get AI query history for an incident",
        description = "Devuelve todas las consultas enviadas a n8n para un incidente, junto con sus respuestas, fuentes, similitudes y calificaciones."
    )
    @GetMapping("/incidents/{incidentId}/history")
    public ResponseEntity<List<AiQueryHistoryResponseDTO>> getIncidentAiHistory(
            @PathVariable Long incidentId) {
        List<AiQueryHistoryResponseDTO> history = aiAssistanceService.getIncidentAiHistory(incidentId);
        return ResponseEntity.ok(history);
    }
}
