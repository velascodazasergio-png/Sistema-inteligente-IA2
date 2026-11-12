// Analítica (RF06): indicadores y distribuciones filtrables por fecha,
// categoría y prioridad.
// - KPI, ranking de categorías y tendencia semanal: GET /api/v1/analytics/dashboard
//   (calculados con consultas en la base de datos, con los mismos filtros).
// - Distribución por estado, prioridad y tiempos: GET /api/v1/incidents con los
//   mismos filtros, contados en el cliente.

import { pageHeader, icon, escapeHtml } from '../ui.js';
import { kpiCard, animateCounters } from '../components/kpi-card.js';
import { barChart, columnChart, lineChart, donutChart } from '../components/charts.js';
import { skeletonKpis, skeletonChart } from '../components/skeleton.js';
import { errorState } from '../components/states.js';
import { getDashboard } from '../services/analytics.service.js';
import { listIncidents } from '../services/incidents.service.js';
import { listCategories } from '../services/catalogs.service.js';
import { PRIORIDAD, optionsHtml } from '../catalogs.js';
import { formatYearWeek } from '../format.js';
import { distributions } from '../analytics-utils.js';

const chartCard = (id, title, subtitle, wide = false) => `
  <section class="card chart-card${wide ? ' chart-card--wide' : ''}" aria-labelledby="${id}-title">
    <div class="chart-card__head"><div>
      <h2 class="card__title" id="${id}-title">${escapeHtml(title)}</h2>
      <p class="card__subtitle">${escapeHtml(subtitle)}</p>
    </div></div>
    <div id="${id}">${skeletonChart(220)}</div>
  </section>`;

export async function render(container, { route, signal }) {
  container.innerHTML = `
    ${pageHeader(route)}
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

      <div class="dash-grid">
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

  function draw(dashboard, dist) {
    kpis.innerHTML = `
      <div class="kpi-grid">
        ${kpiCard({ label: 'Incidentes', value: dashboard.totalIncidents, icon: 'i-inbox' })}
        ${kpiCard({ label: 'Resueltos o cerrados', value: dashboard.resolvedIncidents, icon: 'i-check-circle', tone: 'success' })}
        ${kpiCard({ label: 'Pendientes', value: dist.pending, icon: 'i-hourglass', tone: 'warning' })}
        ${kpiCard({ label: 'Porcentaje de resolución', value: dashboard.resolutionRatePercentage, format: 'percent', icon: 'i-percent', tone: 'accent', progress: dashboard.resolutionRatePercentage })}
        ${kpiCard({ label: 'Tiempo promedio de atención', value: dashboard.averageResolutionTimeHours, format: 'duration', icon: 'i-clock' })}
        ${kpiCard({ label: 'Críticos sin resolver', value: dist.critical, icon: 'i-alert', tone: 'danger' })}
      </div>`;
    animateCounters(kpis);

    const weeks = dashboard.weeklyTrends ?? [];
    const weekly = {
      labels: weeks.map((w) => formatYearWeek(w.yearWeek)),
      series: [
        { name: 'Registrados', values: weeks.map((w) => w.totalReported) },
        { name: 'Resueltos', values: weeks.map((w) => w.totalResolved) },
      ],
    };
    const byCategory = (dashboard.topCategories ?? []).map((c) => ({ label: c.categoryName, value: c.incidentCount }));

    if (charts) {
      charts.weekly.update(weekly);
      charts.category.update(byCategory);
      charts.times.update(dist.times);
      charts.status.update(dist.byStatus);
      charts.priority.update(dist.byPriority);
      return;
    }
    const $ = (id) => container.querySelector(id);
    charts = {
      weekly: lineChart($('#ac-weekly'), { title: 'Tendencia semanal', labelHeader: 'Semana', integer: true, data: weekly, signal }),
      category: barChart($('#ac-category'), { title: 'Incidentes por categoría', data: byCategory, signal }),
      times: columnChart($('#ac-times'), { title: 'Distribución de tiempos de resolución', labelHeader: 'Rango', integer: true, data: dist.times, signal }),
      status: donutChart($('#ac-status'), { title: 'Incidentes por estado', centerLabel: 'incidentes', data: dist.byStatus, signal }),
      priority: donutChart($('#ac-priority'), { title: 'Incidentes por prioridad', centerLabel: 'incidentes', data: dist.byPriority, signal }),
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
    let dashboard;
    let incidents;
    try {
      [dashboard, incidents] = await Promise.all([getDashboard(filters, { signal }), listIncidents(filters, { signal })]);
    } catch (error) {
      if (signal.aborted) return;
      scope.textContent = '';
      errorState(kpis, error, load);
      return;
    }
    if (signal.aborted) return;
    const active = Object.values(filters).filter(Boolean).length;
    scope.textContent = active
      ? `${dashboard.totalIncidents} incidentes con ${active} ${active === 1 ? 'filtro' : 'filtros'}`
      : `${dashboard.totalIncidents} incidentes, sin filtros`;
    draw(dashboard, distributions(incidents));
  }

  form.addEventListener('submit', (event) => { event.preventDefault(); load(); });
  form.addEventListener('reset', () => setTimeout(load));

  listCategories({}, { signal })
    .then((categories) => {
      form.elements.categoryId.insertAdjacentHTML('beforeend', categories
        .map((c) => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join(''));
    })
    .catch(() => {}); // Si falla, el filtro queda en "Todas"; el error principal lo muestra load().

  await load();
}
