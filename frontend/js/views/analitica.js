// Analítica (RF06): indicadores y distribuciones filtrables por fecha,
// categoría y prioridad.
// Los endpoints de /api/v1/analytics no aceptan filtros todavía, así que con
// filtros los indicadores se calculan sobre GET /api/v1/incidents (datos reales
// ya filtrados por el backend), con las mismas reglas que usa el backend:
//   resuelto = RESOLVED o CLOSED; tiempo de atención = updatedAt − createdAt.

import { pageHeader, icon, escapeHtml } from '../ui.js';
import { kpiCard, animateCounters } from '../components/kpi-card.js';
import { barChart, columnChart, lineChart, donutChart } from '../components/charts.js';
import { skeletonKpis, skeletonChart } from '../components/skeleton.js';
import { errorState } from '../components/states.js';
import { listIncidents, catalogsFromIncidents } from '../services/incidents.service.js';
import { ESTADO_INCIDENTE, PRIORIDAD, ESTADOS_FINALES, optionsHtml } from '../catalogs.js';
import { parseDate, countBy } from '../format.js';

const HOUR = 3600 * 1000;
const TIME_BUCKETS = [
  { label: '< 1 h', max: 1 },
  { label: '1–4 h', max: 4 },
  { label: '4–8 h', max: 8 },
  { label: '8–24 h', max: 24 },
  { label: '1–3 d', max: 72 },
  { label: '> 3 d', max: Infinity },
];

const chartCard = (id, title, subtitle, wide = false) => `
  <section class="card chart-card${wide ? ' chart-card--wide' : ''}" aria-labelledby="${id}-title">
    <div class="chart-card__head"><div>
      <h2 class="card__title" id="${id}-title">${escapeHtml(title)}</h2>
      <p class="card__subtitle">${escapeHtml(subtitle)}</p>
    </div></div>
    <div id="${id}">${skeletonChart(220)}</div>
  </section>`;

const resolutionHours = (i) => {
  const start = parseDate(i.createdAt);
  const end = parseDate(i.updatedAt);
  return start && end && end >= start ? (end - start) / HOUR : null;
};

// Lunes de la semana de una fecha, como clave AAAA-MM-DD.
function weekKey(date) {
  const d = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
const weekLabel = new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short' });

function compute(incidents) {
  const resolved = incidents.filter((i) => ESTADOS_FINALES.includes(i.status));
  const hours = resolved.map(resolutionHours).filter((h) => h != null);
  const weeks = new Map();
  incidents.forEach((i) => {
    const date = parseDate(i.createdAt);
    if (!date) return;
    const key = weekKey(date);
    const entry = weeks.get(key) ?? { reported: 0, resolved: 0 };
    entry.reported += 1;
    if (ESTADOS_FINALES.includes(i.status)) entry.resolved += 1;
    weeks.set(key, entry);
  });
  const weekly = [...weeks].sort(([a], [b]) => a.localeCompare(b));

  return {
    total: incidents.length,
    resolved: resolved.length,
    pending: incidents.length - resolved.length,
    rate: incidents.length ? (resolved.length / incidents.length) * 100 : null,
    avgHours: hours.length ? hours.reduce((a, b) => a + b, 0) / hours.length : null,
    critical: incidents.filter((i) => i.priority === 'CRITICAL' && !ESTADOS_FINALES.includes(i.status)).length,
    byCategory: countBy(incidents, (i) => i.categoryName ?? 'Sin categoría').map(({ key, count }) => ({ label: key, value: count })),
    byStatus: Object.entries(ESTADO_INCIDENTE).map(([k, e]) => ({ label: e.label, color: e.color, value: incidents.filter((i) => i.status === k).length })),
    byPriority: Object.entries(PRIORIDAD).map(([k, e]) => ({ label: e.label, color: e.color, value: incidents.filter((i) => i.priority === k).length })),
    times: TIME_BUCKETS.map((b, idx) => ({
      label: b.label,
      value: hours.filter((h) => h < b.max && h >= (TIME_BUCKETS[idx - 1]?.max ?? 0)).length,
    })),
    weekly: {
      labels: weekly.map(([key]) => weekLabel.format(new Date(`${key}T00:00:00`))),
      series: [
        { name: 'Registrados', values: weekly.map(([, w]) => w.reported) },
        { name: 'Resueltos', values: weekly.map(([, w]) => w.resolved) },
      ],
    },
  };
}

export async function render(container, { route, signal }) {
  container.innerHTML = `
    ${pageHeader({ eyebrow: route.rf, title: route.title, description: route.description })}
    <div class="stack" id="analytics">
      <form class="card filters" id="a-filters" aria-label="Filtros de analítica">
        <div class="filters__grid">
          <div class="field"><label for="a-start">Fecha inicial</label><input class="input" id="a-start" name="startDate" type="date"></div>
          <div class="field"><label for="a-end">Fecha final</label><input class="input" id="a-end" name="endDate" type="date"></div>
          <div class="field"><label for="a-category">Categoría</label><select class="input" id="a-category" name="categoryId"><option value="">Todas</option></select></div>
          <div class="field"><label for="a-priority">Prioridad</label>
            <select class="input" id="a-priority" name="priority"><option value="">Todas</option>${optionsHtml(PRIORIDAD)}</select></div>
        </div>
        <p class="field__error" id="a-error" aria-live="polite"></p>
        <div class="form__actions">
          <button class="btn btn--primary" type="submit">${icon('i-filter')}Aplicar filtros</button>
          <button class="btn btn--ghost" type="reset">Limpiar</button>
          <span class="cell-muted" id="a-scope" aria-live="polite"></span>
        </div>
      </form>

      <div id="a-kpis">${skeletonKpis(6)}</div>

      <div class="dash-grid" id="a-charts">
        ${chartCard('ac-weekly', 'Tendencia semanal', 'Registrados frente a resueltos', true)}
        ${chartCard('ac-category', 'Categorías con más problemas', 'Número de incidentes')}
        ${chartCard('ac-times', 'Tiempos de resolución', 'Incidentes resueltos según cuánto tardaron')}
        ${chartCard('ac-status', 'Por estado', 'Distribución actual')}
        ${chartCard('ac-priority', 'Por prioridad', 'Distribución por nivel')}
      </div>
    </div>`;

  const form = container.querySelector('#a-filters');
  const errorEl = container.querySelector('#a-error');
  const scope = container.querySelector('#a-scope');
  const kpis = container.querySelector('#a-kpis');
  let charts = null;

  function draw(stats) {
    kpis.innerHTML = `
      <div class="kpi-grid">
        ${kpiCard({ label: 'Incidentes', value: stats.total, icon: 'i-inbox' })}
        ${kpiCard({ label: 'Resueltos o cerrados', value: stats.resolved, icon: 'i-check-circle', tone: 'success' })}
        ${kpiCard({ label: 'Pendientes', value: stats.pending, icon: 'i-hourglass', tone: 'warning' })}
        ${kpiCard({ label: 'Porcentaje de resolución', value: stats.rate, format: 'percent', icon: 'i-percent', tone: 'accent', progress: stats.rate })}
        ${kpiCard({ label: 'Tiempo promedio de atención', value: stats.avgHours, format: 'duration', icon: 'i-clock' })}
        ${kpiCard({ label: 'Críticos sin resolver', value: stats.critical, icon: 'i-alert', tone: 'danger' })}
      </div>`;
    animateCounters(kpis);

    if (charts) {
      charts.weekly.update(stats.weekly);
      charts.category.update(stats.byCategory);
      charts.times.update(stats.times);
      charts.status.update(stats.byStatus);
      charts.priority.update(stats.byPriority);
      return;
    }
    const $ = (id) => container.querySelector(id);
    charts = {
      weekly: lineChart($('#ac-weekly'), { title: 'Tendencia semanal', labelHeader: 'Semana', integer: true, data: stats.weekly, signal }),
      category: barChart($('#ac-category'), { title: 'Incidentes por categoría', data: stats.byCategory, signal }),
      times: columnChart($('#ac-times'), { title: 'Distribución de tiempos de resolución', labelHeader: 'Rango', integer: true, data: stats.times, signal }),
      status: donutChart($('#ac-status'), { title: 'Incidentes por estado', centerLabel: 'incidentes', data: stats.byStatus, signal }),
      priority: donutChart($('#ac-priority'), { title: 'Incidentes por prioridad', centerLabel: 'incidentes', data: stats.byPriority, signal }),
    };
  }

  async function load() {
    const filters = Object.fromEntries(new FormData(form));
    if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) {
      errorEl.textContent = 'La fecha inicial no puede ser posterior a la final.';
      return;
    }
    errorEl.textContent = '';
    scope.textContent = 'Cargando…';
    let incidents;
    try {
      incidents = await listIncidents(filters, { signal });
    } catch (error) {
      if (signal.aborted) return;
      scope.textContent = '';
      errorState(kpis, error, load);
      return;
    }
    if (signal.aborted) return;
    const active = Object.values(filters).filter(Boolean).length;
    scope.textContent = active ? `${incidents.length} incidentes con ${active} ${active === 1 ? 'filtro' : 'filtros'}` : `${incidents.length} incidentes, sin filtros`;
    draw(compute(incidents));
    return incidents;
  }

  form.addEventListener('submit', (event) => { event.preventDefault(); load(); });
  form.addEventListener('reset', () => setTimeout(load));

  // La primera carga, sin filtros, también llena el selector de categorías.
  const all = await load();
  if (all) {
    form.elements.categoryId.insertAdjacentHTML('beforeend', catalogsFromIncidents(all).categories
      .map((c) => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join(''));
  }
}
