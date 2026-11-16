// Backend value catalogs (Java enums) with their label and color.
// They must match backend/src/main/java/com/mafe/incidentai/domain/enums.

import { escapeHtml } from './ui.js';

// IncidentStatus
export const ESTADO_INCIDENTE = {
  OPEN: { label: 'Abierto', tone: 'info', color: 'var(--chart-1)' },
  IN_AI_ANALYSIS: { label: 'En análisis IA', tone: 'accent', color: 'var(--chart-2)' },
  ASSIGNED_TO_TECH: { label: 'Asignado a técnico', tone: 'warning', color: 'var(--chart-4)' },
  RESOLVED: { label: 'Resuelto', tone: 'success', color: 'var(--chart-3)' },
  CLOSED: { label: 'Cerrado', tone: 'neutral', color: 'var(--text-faint)' },
};

// Priority
export const PRIORIDAD = {
  LOW: { label: 'Baja', tone: 'neutral', color: 'var(--chart-5)' },
  MEDIUM: { label: 'Media', tone: 'info', color: 'var(--chart-1)' },
  HIGH: { label: 'Alta', tone: 'warning', color: 'var(--warning)' },
  CRITICAL: { label: 'Crítica', tone: 'danger', color: 'var(--danger)' },
};

// QueryStatus.
// NO_RESULTS: n8n answered with no relevant documents.
// COMMUNICATION_ERROR: the backend could not deliver the query to n8n.
// FAILED: n8n received the query but failed to process it.
export const ESTADO_CONSULTA = {
  PENDING: { label: 'Pendiente', tone: 'warning' },
  COMPLETED: { label: 'Respondida', tone: 'success' },
  NO_RESULTS: { label: 'Sin resultados', tone: 'neutral' },
  COMMUNICATION_ERROR: { label: 'Error de conexión', tone: 'danger' },
  FAILED: { label: 'Error en la IA', tone: 'danger' },
};

// Statuses in which the incident is no longer being worked on.
export const ESTADOS_FINALES = ['RESOLVED', 'CLOSED'];

// Status to display for a history query (AiQueryHistoryResponseDTO).
// COMPLETED queries without sources (stored before NO_RESULTS existed)
// are also shown as "Sin resultados".
export function estadoConsulta(query) {
  if (query.queryStatus === 'COMPLETED' && !query.aiResponse?.sources?.length) return 'NO_RESULTS';
  return query.queryStatus;
}

// Spanish label of a catalog value
export const labelOf = (catalog, value) => catalog[value]?.label ?? value ?? '—';

// <option> elements for a catalog.
export const optionsHtml = (catalog, selected = '') => Object.entries(catalog)
  .map(([value, { label }]) => `<option value="${value}" ${value === selected ? 'selected' : ''}>${escapeHtml(label)}</option>`)
  .join('');

// Colored badge for a catalog value. Unknown values are
// shown as they are, in gray, so unexpected data is not hidden.
export function badge(catalog, value) {
  const entry = catalog[value];
  const tone = entry?.tone ?? 'neutral';
  const label = entry?.label ?? value ?? '—';
  return `<span class="badge badge--${tone}"><i class="badge__dot" aria-hidden="true"></i>${escapeHtml(label)}</span>`;
}
