package com.mafe.incidentesia.controller;

import com.mafe.incidentesia.dto.AsistenciaRequestDTO;
import com.mafe.incidentesia.dto.RespuestaIADTO;
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

@RestController
@RequestMapping("/api/v1/asistencia-ia")
@Tag(
    name = "Asistencia IA (Contratos n8n)",
    description = "Endpoints base para solicitar analisis de incidentes hacia n8n y registrar las respuestas generadas por la IA"
)
public class AsistenciaIAController {

    @Operation(
        summary = "Solicitar analisis de un incidente a la IA (Disparador hacia n8n)",
        description = "Recibe el contexto del incidente, registra la ConsultaIA en estado PENDIENTE y envia el payload al webhook de n8n."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Solicitud aceptada y enviada al flujo de n8n"),
        @ApiResponse(responseCode = "400", description = "Datos del incidente invalidos o incompletos")
    })
    @PostMapping("/solicitar")
    public ResponseEntity<Map<String, Object>> solicitarAsistencia(
            @Valid @RequestBody AsistenciaRequestDTO request) {

        // Esqueleto Dia 1: Respuesta simulada para validar contrato en Swagger UI
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "mensaje", "Solicitud de asistencia enviada a n8n correctamente",
                "incidenteId", request.incidenteId(),
                "estadoConsulta", "PENDIENTE",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @Operation(
        summary = "Registrar respuesta de la IA (Webhook de retorno desde n8n)",
        description = "Recibe el diagnostico, nivel de confianza, tiempo de procesamiento y fuentes documentales desde n8n."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "RespuestaIA y sus fuentes registradas exitosamente"),
        @ApiResponse(responseCode = "400", description = "Payload de respuesta invalido")
    })
    @PostMapping("/respuestas")
    public ResponseEntity<Map<String, Object>> registrarRespuestaIA(
            @Valid @RequestBody RespuestaIADTO respuestaDto) {

        int totalFuentes = respuestaDto.fuentes() != null ? respuestaDto.fuentes().size() : 0;

        // Esqueleto Dia 1: Confirmacion de recepcion del contrato desde n8n
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Respuesta de IA registrada con exito",
                "consultaIaId", respuestaDto.consultaIaId(),
                "fuentesRegistradas", totalFuentes,
                "estadoConsulta", "COMPLETADA",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}

