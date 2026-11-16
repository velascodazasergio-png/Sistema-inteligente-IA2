// AI assistant service (RF05) and history/rating (RF08).
// Contract: backend/src/main/java/com/mafe/incidentai/controller/AiAssistanceController.java
//
// POST /api/v1/ai-assistance/responses is used by n8n to send the response
// back to the backend. The frontend must not call it.

import { api, ApiError } from '../api.js';

const BASE = '/api/v1/ai-assistance';

// Same minimum as the backend and the n8n RAG workflow.
export const MIN_DESCRIPTION = 20;

/**
 * POST /request — AiAssistanceRequestDTO, built from the incident.
 * 202 response: { message, aiQueryId, incidentId, queryStatus, timestamp }
 */
export function requestAssistance(incident, additionalContext = '', options) {
  // Business rule: an empty description must not trigger the AI workflow.
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

/** GET /history — every query, newest first. Optional QueryStatus filter. */
export const getAiHistory = ({ status } = {}, options) =>
  api.get(`${BASE}/history`, { ...options, query: { status: status || undefined } });

/** GET /incidents/{id}/history — AiQueryHistoryResponseDTO[] */
export const getIncidentHistory = (incidentId, options) =>
  api.get(`${BASE}/incidents/${incidentId}/history`, options);

/** POST /responses/{aiResponseId}/ratings — CreateRatingRequestDTO. 409 if already rated. */
export function rateResponse(aiResponseId, { evaluatorUserId, score, helpful, feedbackComment }, options) {
  return api.post(`${BASE}/responses/${aiResponseId}/ratings`, {
    evaluatorUserId: Number(evaluatorUserId),
    score,
    helpful,
    feedbackComment: feedbackComment?.trim() || null,
  }, options);
}
