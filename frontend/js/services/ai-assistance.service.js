// Servicio del asistente IA (RF05) e historial/calificación (RF08).
// Contrato: backend/src/main/java/com/mafe/incidentai/controller/AiAssistanceController.java
//
// POST /api/v1/ai-assistance/responses lo usa n8n para devolver la respuesta
// al backend. El frontend no debe llamarlo.

import { api, ApiError } from '../api.js';

const BASE = '/api/v1/ai-assistance';

// Mismo mínimo que el backend y el flujo RAG de n8n.
export const MIN_DESCRIPTION = 20;

/**
 * POST /request — AiAssistanceRequestDTO, armado a partir del incidente.
 * Respuesta 202: { message, aiQueryId, incidentId, queryStatus, timestamp }
 */
export function requestAssistance(incident, additionalContext = '', options) {
  // Regla de negocio: una descripción vacía no debe disparar el flujo inteligente.
  if (!incident?.description?.trim()) {
    return Promise.reject(new ApiError('El incidente no tiene descripción: no se puede consultar a la IA.', { code: 'VALIDATION' }));
  }
  if (incident.description.trim().length < MIN_DESCRIPTION) {
    return Promise.reject(new ApiError(`La descripción debe tener al menos ${MIN_DESCRIPTION} caracteres para buscar en la base de conocimiento.`, { code: 'VALIDATION' }));
  }
  return api.post(`${BASE}/request`, {
    incidentId: incident.id,
    incidentTitle: incident.title,
    detailedDescription: incident.description.trim(),
    categoryName: incident.categoryName,
    priority: incident.priority,
    additionalPromptContext: additionalContext.trim() || null,
  }, options);
}

/** GET /history — todas las consultas, de la más reciente a la más antigua. Filtro opcional por QueryStatus. */
export const getAiHistory = ({ status } = {}, options) =>
  api.get(`${BASE}/history`, { ...options, query: { status: status || undefined } });

/** GET /incidents/{id}/history — AiQueryHistoryResponseDTO[] */
export const getIncidentHistory = (incidentId, options) =>
  api.get(`${BASE}/incidents/${incidentId}/history`, options);

/** POST /responses/{aiResponseId}/ratings — CreateRatingRequestDTO. 409 si ya fue calificada. */
export function rateResponse(aiResponseId, { evaluatorUserId, score, helpful, feedbackComment }, options) {
  return api.post(`${BASE}/responses/${aiResponseId}/ratings`, {
    evaluatorUserId: Number(evaluatorUserId),
    score,
    helpful,
    feedbackComment: feedbackComment?.trim() || null,
  }, options);
}
