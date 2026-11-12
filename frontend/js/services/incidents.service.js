// Servicio de incidentes (RF01).
// Contrato: backend/src/main/java/com/mafe/incidentai/controller/IncidentController.java

import { api } from '../api.js';

const BASE = '/api/v1/incidents';

// Fecha de un <input type="date"> (AAAA-MM-DD) → LocalDateTime ISO que espera Spring.
const startOfDay = (date) => (date ? `${date}T00:00:00` : undefined);
const endOfDay = (date) => (date ? `${date}T23:59:59` : undefined);

/**
 * GET /api/v1/incidents — todos los filtros son opcionales.
 * @param {{categoryId?, priority?, userId?, startDate?, endDate?}} filters fechas en AAAA-MM-DD
 * @returns {Promise<IncidentResponseDTO[]>}
 */
export function listIncidents(filters = {}, options) {
  return api.get(BASE, {
    ...options,
    query: {
      categoryId: filters.categoryId || undefined,
      priority: filters.priority || undefined,
      userId: filters.userId || undefined,
      startDate: startOfDay(filters.startDate),
      endDate: endOfDay(filters.endDate),
    },
  });
}

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

/**
 * El backend aún no expone endpoints de categorías ni de usuarios.
 * Mientras tanto, los catálogos se obtienen de los incidentes ya registrados
 * (datos reales, pero solo incluyen categorías y usuarios que tengan incidentes).
 */
export function catalogsFromIncidents(incidents) {
  const categories = new Map();
  const users = new Map();
  incidents.forEach((i) => {
    if (i.categoryId != null) categories.set(i.categoryId, i.categoryName);
    if (i.userId != null) users.set(i.userId, i.userFullName);
  });
  const sorted = (map) => [...map].map(([id, name]) => ({ id, name: name ?? `#${id}` }))
    .sort((a, b) => a.name.localeCompare(b.name, 'es'));
  return { categories: sorted(categories), users: sorted(users) };
}
