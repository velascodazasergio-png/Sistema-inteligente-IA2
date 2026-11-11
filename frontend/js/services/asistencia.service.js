// Servicio del asistente IA (RF05).
// Contrato: backend/src/main/java/com/mafe/incidentesia/dto/AsistenciaRequestDTO.java
//
// Nota: POST /api/v1/asistencia-ia/respuestas lo usa n8n para devolver la
// respuesta al backend. El frontend no debe llamarlo.

import { api, ApiError } from '../api.js';

const BASE = '/api/v1/asistencia-ia';

export function solicitarAsistencia(datos, options) {
  const {
    incidenteId,
    tituloIncidente,
    descripcionDetallada,
    categoriaNombre,
    prioridad,
    contextoAdicionalPrompt,
  } = datos;

  // Regla de negocio: una descripción vacía no debe disparar el flujo inteligente.
  if (!descripcionDetallada?.trim()) {
    return Promise.reject(new ApiError('Describe el incidente antes de consultar a la IA.', { code: 'VALIDATION' }));
  }

  return api.post(`${BASE}/solicitar`, {
    incidenteId,
    tituloIncidente: tituloIncidente?.trim(),
    descripcionDetallada: descripcionDetallada.trim(),
    categoriaNombre,
    prioridad,
    contextoAdicionalPrompt: contextoAdicionalPrompt?.trim() || null,
  }, options);
}
