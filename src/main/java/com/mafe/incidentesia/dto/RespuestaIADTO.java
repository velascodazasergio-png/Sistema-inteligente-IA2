package com.mafe.incidentesia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Contrato REST que n8n envia de vuelta a Spring Boot con el diagnostico de la IA y sus fuentes")
public record RespuestaIADTO(

        @Schema(description = "ID de la ConsultaIA original que disparo el analisis", example = "55")
        @NotNull(message = "El ID de la consulta IA es obligatorio para asociar la respuesta")
        Long consultaIaId,

        @Schema(description = "Diagnostico probable generado por el modelo de IA", example = "Posible bloqueo del puerto UDP 500/4500 (IPSec NAT-T) en el firewall perimetral")
        @NotBlank(message = "El diagnostico sugerido no puede estar vacio")
        String diagnosticoSugerido,

        @Schema(description = "Pasos recomendados para resolver el incidente", example = "1. Verificar reglas UDP 500 y 4500. 2. Reiniciar servicio IKEEXT en el cliente.")
        String pasosSolucion,

        @Schema(description = "Nivel de confianza del modelo entre 0.0 y 1.0", example = "0.92")
        @DecimalMin(value = "0.0")
        @DecimalMax(value = "1.0")
        Double nivelConfianza,

        @Schema(description = "Tiempo total que tardo el flujo de n8n en milisegundos", example = "2340")
        Long tiempoProcesamientoMs,

        @Schema(description = "Lista de documentos o tickets historicos que respaldan la respuesta (RAG)")
        @Valid
        List<FuenteDTO> fuentes
) {
    @Schema(description = "Fuente documental o ticket historico citado por la IA")
    public record FuenteDTO(
            @Schema(description = "Titulo del manual o ticket de referencia", example = "Runbook VPN IPSec v3.1")
            @NotBlank(message = "El titulo de la fuente es obligatorio")
            String tituloFuente,

            @Schema(description = "URL o identificador del documento en la base de conocimiento", example = "https://kb.empresa.internal/redes/vpn-error-809")
            String referenciaUrl,

            @Schema(description = "Fragmento textual extraido por el flujo RAG en n8n", example = "El error 809 ocurre cuando los paquetes IKE quedan bloqueados por NAT-T...")
            String fragmentoCitado
    ) {}
}

