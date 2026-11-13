// Cálculos compartidos por el Dashboard y Analítica sobre datos reales del backend.

import { ESTADO_INCIDENTE, PRIORIDAD, ESTADOS_FINALES } from './catalogs.js';
import { parseDate } from './format.js';

const HOUR = 3600 * 1000;
const DAY = 24 * HOUR;

export const TIME_BUCKETS = [
  { label: '< 1 h', max: 1 },
  { label: '1–4 h', max: 4 },
  { label: '4–8 h', max: 8 },
  { label: '8–24 h', max: 24 },
  { label: '1–3 d', max: 72 },
  { label: '> 3 d', max: Infinity },
];

// Misma regla que el backend: tiempo de atención = updatedAt − createdAt.
export const resolutionHours = (i) => {
  const start = parseDate(i.createdAt);
  const end = parseDate(i.updatedAt);
  return start && end && end >= start ? (end - start) / HOUR : null;
};

// Distribuciones que el backend no agrega: estado, prioridad y tiempos de resolución.
export function distributions(incidents) {
  const open = (i) => !ESTADOS_FINALES.includes(i.status);
  const hours = incidents.filter((i) => !open(i)).map(resolutionHours).filter((h) => h != null);
  return {
    pending: incidents.filter(open).length,
    critical: incidents.filter((i) => i.priority === 'CRITICAL' && open(i)).length,
    byStatus: Object.entries(ESTADO_INCIDENTE).map(([k, e]) => ({ label: e.label, color: e.color, value: incidents.filter((i) => i.status === k).length })),
    byPriority: Object.entries(PRIORIDAD).map(([k, e]) => ({ label: e.label, color: e.color, value: incidents.filter((i) => i.priority === k).length })),
    times: TIME_BUCKETS.map((b, idx) => ({
      label: b.label,
      value: hours.filter((h) => h < b.max && h >= (TIME_BUCKETS[idx - 1]?.max ?? 0)).length,
    })),
  };
}

// AAAA-MM-DD ↔ Date local
const toDate = (iso) => new Date(`${iso}T00:00:00`);
export const toIso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

// Rango de "últimos N días" terminando hoy.
export function lastDays(days) {
  const end = new Date();
  const start = new Date(end.getTime() - (days - 1) * DAY);
  return { startDate: toIso(start), endDate: toIso(end) };
}

// Periodo anterior de la misma duración: 1–30 nov → 2–31 oct.
export function previousPeriod(startDate, endDate) {
  if (!startDate || !endDate) return null;
  const start = toDate(startDate);
  const end = toDate(endDate);
  const days = Math.round((end - start) / DAY) + 1;
  const prevEnd = new Date(start.getTime() - DAY);
  const prevStart = new Date(prevEnd.getTime() - (days - 1) * DAY);
  return { startDate: toIso(prevStart), endDate: toIso(prevEnd), days };
}

// Variación porcentual; null si no hay base de comparación.
export const pctChange = (current, previous) =>
  (Number.isFinite(current) && Number.isFinite(previous) && previous !== 0 ? ((current - previous) / previous) * 100 : null);

// "2026-10" → "oct 2026"
const monthFmt = new Intl.DateTimeFormat('es-CO', { month: 'short', year: 'numeric' });
export const formatYearMonth = (value) => {
  const [y, m] = String(value ?? '').split('-').map(Number);
  return y && m ? monthFmt.format(new Date(y, m - 1, 1)) : (value ?? '—');
};
