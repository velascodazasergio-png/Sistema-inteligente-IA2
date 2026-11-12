package com.mafe.incidentai.controller;

import com.mafe.incidentai.dto.AiAssistanceRequestDTO;
import com.mafe.incidentai.dto.AiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

// Controlador REST base para gestionar las solicitudes de asistencia hacia n8n y recibir sus respuestas
@RestController
@RequestMapping("/api/v1/ai-assistance")
@Tag(
    name = "AI Assistance (n8n Contracts)",
    description = "Endpoints base para solicitar analisis de incidentes hacia n8n y registrar las respuestas generadas por la IA"
)
public class AiAssistanceController {

    // Endpoint para disparar el analisis de un incidente hacia el webhook de n8n
    @Operation(
        summary = "Request AI incident analysis (Trigger to n8n)",
        description = "Recibe el contexto del incidente, registra el AiQuery en estado PENDING y envia el payload al webhook de n8n."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Solicitud aceptada y enviada al flujo de n8n"),
        @ApiResponse(responseCode = "400", description = "Datos del incidente invalidos o incompletos")
    })
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestAiAssistance(
            @Valid @RequestBody AiAssistanceRequestDTO request) {

        // Esqueleto Dia 1: Respuesta simulada (202 Accepted) para validar el contrato en Swagger UI
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "message", "AI assistance request successfully sent to n8n",
                "incidentId", request.incidentId(),
                "queryStatus", "PENDING",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    // Endpoint (Webhook callback) donde n8n registra el diagnostico y las fuentes encontradas
    @Operation(
        summary = "Register AI response (Callback webhook from n8n)",
        description = "Recibe el diagnostico, nivel de confianza, tiempo de procesamiento y fuentes documentales desde n8n."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "AiResponse y sus fuentes registradas exitosamente"),
        @ApiResponse(responseCode = "400", description = "Payload de respuesta invalido")
    })
    @PostMapping("/responses")
    public ResponseEntity<Map<String, Object>> registerAiResponse(
            @Valid @RequestBody AiResponseDTO responseDto) {

        // Calculamos la cantidad de fuentes documentales enviadas por n8n
        int totalSources = responseDto.sources() != null ? responseDto.sources().size() : 0;

        // Esqueleto Dia 1: Confirmacion de creacion (201 Created) del contrato devuelto por n8n
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "AI response registered successfully",
                "aiQueryId", responseDto.aiQueryId(),
                "registeredSources", totalSources,
                "queryStatus", "COMPLETED",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}

