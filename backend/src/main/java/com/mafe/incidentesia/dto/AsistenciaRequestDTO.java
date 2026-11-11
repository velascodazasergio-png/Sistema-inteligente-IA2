package com.mafe.incidentesia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Contrato REST de entrada/salida hacia el webhook de n8n para solicitar analisis de un incidente")
public record AsistenciaRequestDTO(

        @Schema(description = "ID unico del incidente registrado en el sistema", example = "101")
        @NotNull(message = "El ID del incidente es obligatorio")
        Long incidenteId,

        @Schema(description = "Titulo breve del incidente", example = "Caida intermitente en tunel VPN corporativo")
        @NotBlank(message = "El titulo del incidente no puede estar vacio")
        String tituloIncidente,

        @Schema(description = "Descripcion tecnica detallada del problema reportado", example = "Los usuarios remotos reciben error 809 al conectar desde clientes Windows 11")
        @NotBlank(message = "La descripcion detallada es obligatoria")
        String descripcionDetallada,

        @Schema(description = "Nombre de la categoria del incidente", example = "Redes y Conectividad")
        @NotBlank(message = "La categoria es obligatoria")
        String categoriaNombre,

        @Schema(description = "Nivel de prioridad del incidente", example = "ALTA")
        String prioridad,

        @Schema(description = "Instrucciones o contexto adicional para refinar el prompt enviado al agente en n8n", example = "Verificar si coincide con el cambio de certificados de ayer")
        String contextoAdicionalPrompt
) {}

