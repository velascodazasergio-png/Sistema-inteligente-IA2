// Dashboard (RF07): estado general del soporte.
// Indicadores: GET /api/v1/analytics/dashboard (calculados con SQL en el backend).
// Distribución por estado y prioridad: GET /api/v1/incidents (conteo en el cliente).

import { pageHeader, icon, escapeHtml } from '../ui.js';
import { kpiCard, animateCounters } from '../components/kpi-card.js';
import { barChart, lineChart, donutChart } from '../components/charts.js';
import { skeletonKpis, skeletonChart, skeletonLines } from '../components/skeleton.js';
import { errorState } from '../components/states.js';
import { getDashboard } from '../services/analytics.service.js';
import { listIncidents } from '../services/incidents.service.js';
import { ESTADO_INCIDENTE, PRIORIDAD, badge } from '../catalogs.js';
import { formatDateTime, formatYearWeek, parseDate } from '../format.js';

const chartCard = (id, title, subtitle, wide = false) => `
  <section class="card chart-card${wide ? ' chart-card--wide' : ''}" aria-labelledby="${id}-title">
    <div class="chart-card__head">
      <div>
        <h2 class="card__title" id="${id}-title">${escapeHtml(title)}</h2>
        <p class="card__subtitle">${escapeHtml(subtitle)}</p>
      </div>
    </div>
    <div id="${id}">${skeletonChart(220)}</div>
  </section>`;

// Conteo por cada valor del catálogo, en el orden del catálogo.
const distribution = (incidents, catalog, field) => Object.entries(catalog).map(([value, entry]) => ({
  label: entry.label,
  value: incidents.filter((i) => i[field] === value).length,
  color: entry.color,
}));

export async function render(container, { route, signal }) {
  container.innerHTML = `
    ${pageHeader({ eyebrow: route.rf, title: route.title, description: route.description })}
    <div class="stack">
      <div id="dash-kpis">${skeletonKpis(6)}</div>
      <div class="dash-grid">
        ${chartCard('dash-weekly', 'Evolución semanal', 'Incidentes registrados y resueltos por semana', true)}
        ${chartCard('dash-categories', 'Categorías con más incidentes', 'Ranking calculado en el backend')}
        ${chartCard('dash-status', 'Incidentes por estado', 'Situación actual de todos los incidentes')}
        ${chartCard('dash-priority', 'Incidentes por prioridad', 'Volumen según nivel de prioridad')}
        <section class="card" aria-labelledby="dash-recent-title">
          <div class="chart-card__head">
            <div>
              <h2 class="card__title" id="dash-recent-title">Incidentes recientes</h2>
              <p class="card__subtitle">Últimos registros</p>
            </div>
            <a class="btn btn--ghost" href="#/incidentes">Ver todos ${icon('i-arrow-right')}</a>
          </div>
          <div id="dash-recent">${skeletonLines(5)}</div>
        </section>
      </div>
    </div>`;

  const body = container.querySelector('.stack');

  const load = async () => {
    let dashboard;
    let incidents;
    try {
      [dashboard, incidents] = await Promise.all([getDashboard({ signal }), listIncidents({}, { signal })]);
    } catch (error) {
      if (signal.aborted) return;
      errorState(body, error, () => render(container, { route, signal }));
      return;
    }
    if (signal.aborted) return;

    const pending = (dashboard.totalIncidents ?? 0) - (dashboard.resolvedIncidents ?? 0);
    const kpis = container.querySelector('#dash-kpis');
    kpis.innerHTML = `
      <div class="kpi-grid">
        ${kpiCard({ label: 'Total de incidentes', value: dashboard.totalIncidents, icon: 'i-inbox' })}
        ${kpiCard({ label: 'Pendientes', value: pending, icon: 'i-hourglass', tone: 'warning', hint: 'Sin resolver ni cerrar' })}
        ${kpiCard({ label: 'Resueltos o cerrados', value: dashboard.resolvedIncidents, icon: 'i-check-circle', tone: 'success' })}
        ${kpiCard({ label: 'Porcentaje de resolución', value: dashboard.resolutionRatePercentage, format: 'percent', icon: 'i-percent', tone: 'accent', progress: dashboard.resolutionRatePercentage })}
        ${kpiCard({ label: 'Tiempo promedio de atención', value: dashboard.averageResolutionTimeHours, format: 'duration', icon: 'i-clock', hint: 'Desde el registro hasta la resolución' })}
        ${kpiCard({ label: 'Calificación de la IA', value: dashboard.averageAiRatingScore, icon: 'i-star', tone: 'accent', hint: 'Promedio sobre 5 estrellas' })}
      </div>`;
    animateCounters(kpis);

    const weeks = dashboard.weeklyTrends ?? [];
    lineChart(container.querySelector('#dash-weekly'), {
      title: 'Evolución semanal de incidentes',
      labelHeader: 'Semana',
      integer: true,
      data: {
        labels: weeks.map((w) => formatYearWeek(w.yearWeek)),
        series: [
          { name: 'Registrados', values: weeks.map((w) => w.totalReported) },
          { name: 'Resueltos', values: weeks.map((w) => w.totalResolved) },
        ],
      },
      signal,
    });

    barChart(container.querySelector('#dash-categories'), {
      title: 'Incidentes por categoría',
      data: (dashboard.topCategories ?? []).map((c) => ({ label: c.categoryName, value: c.incidentCount })),
      signal,
    });

    donutChart(container.querySelector('#dash-status'), {
      title: 'Incidentes por estado',
      centerLabel: 'incidentes',
      data: distribution(incidents, ESTADO_INCIDENTE, 'status'),
      signal,
    });

    donutChart(container.querySelector('#dash-priority'), {
      title: 'Incidentes por prioridad',
      centerLabel: 'incidentes',
      data: distribution(incidents, PRIORIDAD, 'priority'),
      signal,
    });

    const recent = [...incidents]
      .sort((a, b) => (parseDate(b.createdAt) ?? 0) - (parseDate(a.createdAt) ?? 0))
      .slice(0, 6);
    container.querySelector('#dash-recent').innerHTML = recent.length ? `
      <ul class="recent-list">
        ${recent.map((i) => `
          <li>
            <a class="recent-item" href="#/incidentes?id=${i.id}">
              <span class="recent-item__main">
                <span class="cell-title">${escapeHtml(i.title)}</span>
                <span class="cell-muted">#${i.id} · ${escapeHtml(i.categoryName ?? '')} · ${formatDateTime(i.createdAt)}</span>
              </span>
              <span class="recent-item__badges">${badge(PRIORIDAD, i.priority)}${badge(ESTADO_INCIDENTE, i.status)}</span>
            </a>
          </li>`).join('')}
      </ul>` : '<p class="cell-muted">Aún no hay incidentes registrados.</p>';
  };

  await load();
}
