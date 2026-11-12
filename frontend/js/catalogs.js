// Catálogos de valores del backend (enums de Java) con su etiqueta y color.
// Deben coincidir con backend/src/main/java/com/mafe/incidentai/domain/enums.

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

// QueryStatus. El enunciado también pide "Sin resultados": el backend aún
// no tiene ese valor, así que la vista lo deduce cuando una respuesta
// completada llega sin fuentes (ver estadoConsulta()).
export const ESTADO_CONSULTA = {
  PENDING: { label: 'Pendiente', tone: 'warning' },
  COMPLETED: { label: 'Respondida', tone: 'success' },
  NO_RESULTS: { label: 'Sin resultados', tone: 'neutral' },
  COMMUNICATION_ERROR: { label: 'Error', tone: 'danger' },
};

// Estados en los que ya no se trabaja el incidente.
export const ESTADOS_FINALES = ['RESOLVED', 'CLOSED'];

// Estado a mostrar para una consulta del historial (AiQueryHistoryResponseDTO).
export function estadoConsulta(query) {
  if (query.queryStatus === 'COMPLETED' && !query.aiResponse?.sources?.length) return 'NO_RESULTS';
  return query.queryStatus;
}

export const labelOf = (catalog, value) => catalog[value]?.label ?? value ?? '—';

// Opciones <option> para un catálogo.
export const optionsHtml = (catalog, selected = '') => Object.entries(catalog)
  .map(([value, { label }]) => `<option value="${value}" ${value === selected ? 'selected' : ''}>${escapeHtml(label)}</option>`)
  .join('');

// Etiqueta de color para un valor de catálogo. Valores desconocidos se
// muestran tal cual, en gris, para no ocultar datos inesperados.
export function badge(catalog, value) {
  const entry = catalog[value];
  const tone = entry?.tone ?? 'neutral';
  const label = entry?.label ?? value ?? '—';
  return `<span class="badge badge--${tone}"><i class="badge__dot" aria-hidden="true"></i>${escapeHtml(label)}</span>`;
}
