// Catálogos de valores del backend (enums de Java) con su etiqueta y color.
// Deben coincidir con backend/src/main/java/com/mafe/incidentesia/domain/enums.

import { escapeHtml } from './ui.js';

export const ESTADO_INCIDENTE = {
  ABIERTO: { label: 'Abierto', tone: 'info', color: 'var(--chart-1)' },
  EN_ANALISIS_IA: { label: 'En análisis IA', tone: 'accent', color: 'var(--chart-2)' },
  ASIGNADO_TECNICO: { label: 'Asignado a técnico', tone: 'warning', color: 'var(--chart-4)' },
  RESUELTO: { label: 'Resuelto', tone: 'success', color: 'var(--chart-3)' },
  CERRADO: { label: 'Cerrado', tone: 'neutral', color: 'var(--text-faint)' },
};

export const PRIORIDAD = {
  BAJA: { label: 'Baja', tone: 'neutral', color: 'var(--chart-5)' },
  MEDIA: { label: 'Media', tone: 'info', color: 'var(--chart-1)' },
  ALTA: { label: 'Alta', tone: 'warning', color: 'var(--warning)' },
  CRITICA: { label: 'Crítica', tone: 'danger', color: 'var(--danger)' },
};

// Valores actuales del enum EstadoConsulta. El enunciado pide
// Pendiente / Respondida / Sin resultados / Error: pendiente de ajustar en backend.
export const ESTADO_CONSULTA = {
  PENDIENTE: { label: 'Pendiente', tone: 'warning' },
  COMPLETADA: { label: 'Respondida', tone: 'success' },
  ERROR_COMUNICACION: { label: 'Error', tone: 'danger' },
};

// Etiqueta de color para un valor de catálogo. Valores desconocidos se
// muestran tal cual, en gris, para no ocultar datos inesperados.
export function badge(catalog, value) {
  const entry = catalog[value];
  const tone = entry?.tone ?? 'neutral';
  const label = entry?.label ?? value ?? '—';
  return `<span class="badge badge--${tone}"><i class="badge__dot" aria-hidden="true"></i>${escapeHtml(label)}</span>`;
}
