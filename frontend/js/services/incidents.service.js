// Servicio de incidentes (RF01).
// Contrato: backend/src/main/java/com/mafe/incidentai/controller/IncidentController.java

import { api } from '../api.js';

const BASE = '/api/v1/incidents';

// Fecha de un <input type="date"> (AAAA-MM-DD) → LocalDateTime ISO que espera Spring.
export const startOfDay = (date) => (date ? `${date}T00:00:00` : undefined);
export const endOfDay = (date) => (date ? `${date}T23:59:59` : undefined);

/**
 * GET /api/v1/incidents — todos los filtros son opcionales.
 * @param {{categoryId?, priority?, status?, userId?, startDate?, endDate?}} filters fechas en AAAA-MM-DD
 * @returns {Promise<IncidentResponseDTO[]>}
 */
export function listIncidents(filters = {}, options) {
  return api.get(BASE, {
    ...options,
    query: {
      categoryId: filters.categoryId || undefined,
      priority: filters.priority || undefined,
      status: filters.status || undefined,
      userId: filters.userId || undefined,
      startDate: startOfDay(filters.startDate),
      endDate: endOfDay(filters.endDate),
    },
  });
}

/** GET /api/v1/incidents/{id} */
export const getIncident = (incidentId, options) => api.get(`${BASE}/${incidentId}`, options);

/** POST /api/v1/incidents — CreateIncidentRequestDTO */
export function createIncident({ title, description, priority, userId, categoryId }, options) {
  return api.post(BASE, {
    title: title.trim(),
    description: description.trim(),
    priority,
    userId: Number(userId),
    categoryId: Number(categoryId),
  }, options);
}

/** PATCH /api/v1/incidents/{id}/status — UpdateIncidentStatusRequestDTO */
export function updateIncidentStatus(incidentId, status, options) {
  return api.patch(`${BASE}/${incidentId}/status`, { status }, options);
}
