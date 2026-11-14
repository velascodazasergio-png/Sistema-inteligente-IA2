package com.mafe.incidentai.controller;

import com.mafe.incidentai.security.AccessPolicy;
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
import java.util.Set;

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
    private final AccessPolicy accessPolicy;

    // 1. Dispara el analisis de un incidente hacia el webhook de n8n y registra AiQuery (PENDING o COMMUNICATION_ERROR)
    @Operation(
        summary = "Request AI incident analysis (Trigger to n8n)",
        description = "Registra la consulta AiQuery, pasa el incidente a IN_AI_ANALYSIS, dispara el cliente HTTP hacia n8n y marca COMMUNICATION_ERROR si ocurre timeout o error."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Solicitud procesada y registrada en base de datos"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos (por ejemplo, descripcion vacia)"),
        @ApiResponse(responseCode = "404", description = "Incidente no encontrado"),
        @ApiResponse(responseCode = "409", description = "El incidente esta cerrado (CLOSED)")
    })
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestAiAssistance(
            @Valid @RequestBody AiAssistanceRequestDTO request) {
        accessPolicy.checkIncident(request.incidentId());
        Map<String, Object> result = aiAssistanceService.requestAiAssistance(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(result);
    }

    // 2. Recibe el callback desde n8n y guarda en cascada AiResponse y sus ResponseSource con similarityScore
    @Operation(
        summary = "Register AI response and sources (Callback webhook from n8n)",
        description = "Guarda en cadena la respuesta de n8n (AiResponse) y sus fuentes (ResponseSource) con su similitud. "
                + "Marca AiQuery como COMPLETED si hay fuentes, o NO_RESULTS si n8n no encontro documentos relevantes. "
                + "Si app.callback.token esta configurado, requiere el encabezado X-Callback-Token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "AiResponse y sus fuentes guardadas en cadena exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
        @ApiResponse(responseCode = "401", description = "Token de callback ausente o incorrecto"),
        @ApiResponse(responseCode = "404", description = "AiQuery original no encontrada"),
        @ApiResponse(responseCode = "409", description = "La consulta ya tiene una respuesta registrada")
    })
    @PostMapping("/responses")
    public ResponseEntity<Map<String, Object>> registerAiResponse(
            @Valid @RequestBody AiResponseDTO responseDto) {
        Map<String, Object> result = aiAssistanceService.registerAiResponse(responseDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    // 2b. Recibe desde n8n el reporte de un fallo al procesar la consulta (embeddings, Qdrant o LLM)
    @Operation(
        summary = "Report an AI processing failure (Callback webhook from n8n)",
        description = "Marca la consulta como FAILED y guarda el codigo y mensaje del fallo. Si app.callback.token esta configurado, requiere el encabezado X-Callback-Token."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fallo registrado"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
        @ApiResponse(responseCode = "401", description = "Token de callback ausente o incorrecto"),
        @ApiResponse(responseCode = "404", description = "AiQuery no encontrada"),
        @ApiResponse(responseCode = "409", description = "La consulta ya tiene una respuesta o el ID no coincide")
    })
    @PostMapping("/queries/{aiQueryId}/failure")
    public ResponseEntity<Map<String, Object>> reportFailure(
            @PathVariable Long aiQueryId,
            @Valid @RequestBody AiFailureReportDTO report) {
        return ResponseEntity.ok(aiAssistanceService.reportFailure(aiQueryId, report));
    }

    // 3. Registra la calificacion (Rating) sobre una respuesta generada por la IA
    @Operation(
        summary = "Rate an AI response",
        description = "Permite a un tecnico o usuario calificar de 1 a 5 la utilidad de una respuesta generada por la IA."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Calificacion registrada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Puntuacion fuera de 1 a 5 o datos incompletos"),
        @ApiResponse(responseCode = "404", description = "Respuesta IA o Usuario evaluador no encontrado"),
        @ApiResponse(responseCode = "409", description = "La respuesta IA ya habia sido calificada previamente")
    })
    @PostMapping("/responses/{aiResponseId}/ratings")
    public ResponseEntity<RatingResponseDTO> rateAiResponse(
            @PathVariable Long aiResponseId,
            @Valid @RequestBody CreateRatingRequestDTO request) {
        // Solo el administrador puede calificar en nombre de otra persona; el resto califica con su usuario
        if (!accessPolicy.isAdmin()) {
            accessPolicy.checkIncident(aiAssistanceService.findIncidentIdOfResponse(aiResponseId));
            request = new CreateRatingRequestDTO(accessPolicy.user().id(), request.score(), request.helpful(),
                    request.feedbackComment());
        }
        RatingResponseDTO response = aiAssistanceService.rateAiResponse(aiResponseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 4. Consulta el historial completo de consultas a la IA (AiQuery) para un incidente
    @Operation(
        summary = "Get AI query history for an incident",
        description = "Devuelve todas las consultas enviadas a n8n para un incidente, junto con sus respuestas, fuentes, similitudes y calificaciones."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historial del incidente"),
        @ApiResponse(responseCode = "404", description = "Incidente no encontrado")
    })
    @GetMapping("/incidents/{incidentId}/history")
    public ResponseEntity<List<AiQueryHistoryResponseDTO>> getIncidentAiHistory(
            @PathVariable Long incidentId) {
        accessPolicy.checkIncident(incidentId);
        List<AiQueryHistoryResponseDTO> history = aiAssistanceService.getIncidentAiHistory(incidentId);
        return ResponseEntity.ok(history);
    }

    // 5. Consulta el historial global de consultas a la IA
    @Operation(
        summary = "Get global AI query history",
        description = "Devuelve todas las consultas a la IA, de la mas reciente a la mas antigua. Filtro opcional por estado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historial global"),
        @ApiResponse(responseCode = "400", description = "Estado de consulta invalido")
    })
    @GetMapping("/history")
    public ResponseEntity<List<AiQueryHistoryResponseDTO>> getAiHistory(
            @RequestParam(required = false) QueryStatus status) {
        List<AiQueryHistoryResponseDTO> history = aiAssistanceService.getAiHistory(status);
        // Un empleado solo ve las consultas de sus propios incidentes
        if (accessPolicy.isEmployee()) {
            Set<Long> own = accessPolicy.ownIncidentIds();
            history = history.stream().filter(q -> own.contains(q.incidentId())).toList();
        }
        return ResponseEntity.ok(history);
    }
}
