// Dashboard (RF07): estado general del soporte, filtrable por fecha, categoría y prioridad.
// - KPI, categorías y evolución semanal/mensual: GET /api/v1/analytics/dashboard (SQL en el backend).
// - Estado, prioridad y tiempos de resolución: GET /api/v1/incidents con los mismos filtros.
// - Comparación temporal: el mismo endpoint para el periodo anterior de igual duración.

import { pageHeader, icon, escapeHtml, avatar } from '../ui.js';
import { kpiCard, animateCounters } from '../components/kpi-card.js';
import { barChart, columnChart, lineChart, donutChart } from '../components/charts.js';
import { skeletonKpis, skeletonChart, skeletonLines } from '../components/skeleton.js';
import { errorState } from '../components/states.js';
import { getDashboard } from '../services/analytics.service.js';
import { listIncidents } from '../services/incidents.service.js';
import { listCategories } from '../services/catalogs.service.js';
import { ESTADO_INCIDENTE, PRIORIDAD, badge, optionsHtml } from '../catalogs.js';
import { formatDateTime, formatYearWeek, parseDate } from '../format.js';
import { distributions, lastDays, previousPeriod, pctChange, formatYearMonth } from '../analytics-utils.js';
import { routeParams } from '../router.js';

const PRESETS = [
  { value: 'all', label: 'Todo el historial' },
  { value: '7', label: 'Últimos 7 días' },
  { value: '30', label: 'Últimos 30 días' },
  { value: '90', label: 'Últimos 90 días' },
  { value: 'custom', label: 'Personalizado' },
];

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

const trendSeries = (rows, labelFn) => ({
  labels: rows.map((r) => labelFn(r)),
  series: [
    { name: 'Registrados', values: rows.map((r) => r.totalReported) },
    { name: 'Resueltos', values: rows.map((r) => r.totalResolved) },
  ],
});

export async function render(container, { route, signal }) {
  container.innerHTML = `
    ${pageHeader(route)}
    <div class="stack" id="dash">
      <section class="hero" id="d-hero" aria-label="Resumen del periodo">${skeletonLines(3)}</section>

      <form class="card filters" id="d-filters" aria-label="Filtros del dashboard">
        <div class="filters__grid">
          <div class="field"><label for="d-period">Periodo</label>
            <select class="input" id="d-period" name="period">${PRESETS.map((p) => `<option value="${p.value}">${p.label}</option>`).join('')}</select></div>
          <div class="field"><label for="d-start">Fecha inicial</label><input class="input" id="d-start" name="startDate" type="date"></div>
          <div class="field"><label for="d-end">Fecha final</label><input class="input" id="d-end" name="endDate" type="date"></div>
          <div class="field"><label for="d-category">Categoría</label><select class="input" id="d-category" name="categoryId"><option value="">Todas</option></select></div>
          <div class="field"><label for="d-priority">Prioridad</label>
            <select class="input" id="d-priority" name="priority"><option value="">Todas</option>${optionsHtml(PRIORIDAD)}</select></div>
        </div>
        <p class="field__error" id="d-error" aria-live="polite"></p>
        <div class="form__actions">
          <button class="btn btn--primary" type="submit">${icon('i-filter')}Aplicar filtros</button>
          <button class="btn btn--ghost" type="reset">Limpiar</button>
          <span class="cell-muted" id="d-scope" aria-live="polite"></span>
        </div>
      </form>

      <div id="d-kpis">${skeletonKpis(6)}</div>

      <div class="dash-grid">
        ${chartCard('dc-weekly', 'Evolución semanal', 'Incidentes registrados y resueltos por semana', true)}
        ${chartCard('dc-monthly', 'Evolución mensual', 'Incidentes registrados y resueltos por mes')}
        ${chartCard('dc-times', 'Tiempos de resolución', 'Incidentes resueltos según cuánto tardaron')}
        ${chartCard('dc-category', 'Incidentes por categoría', 'Ranking calculado en el backend')}
        ${chartCard('dc-status', 'Incidentes por estado', 'Situación actual')}
        ${chartCard('dc-priority', 'Incidentes por prioridad', 'Volumen según nivel de prioridad')}
        <section class="card" aria-labelledby="dc-recent-title">
          <div class="chart-card__head">
            <div>
              <h2 class="card__title" id="dc-recent-title">Incidentes recientes</h2>
              <p class="card__subtitle">Últimos registros del periodo</p>
            </div>
            <a class="btn btn--ghost" href="#/incidentes">Ver todos ${icon('i-arrow-right')}</a>
          </div>
          <div id="dc-recent">${skeletonLines(5)}</div>
        </section>
      </div>
    </div>`;

  const form = container.querySelector('#d-filters');
  const errorEl = container.querySelector('#d-error');
  const scope = container.querySelector('#d-scope');
  const kpis = container.querySelector('#d-kpis');
  const $ = (id) => container.querySelector(id);
  let charts = null;

  // El selector de periodo rellena las fechas; editar una fecha pasa a "Personalizado".
  form.elements.period.addEventListener('change', () => {
    const value = form.elements.period.value;
    if (value === 'all') {
      form.elements.startDate.value = '';
      form.elements.endDate.value = '';
    } else if (value !== 'custom') {
      const range = lastDays(Number(value));
      form.elements.startDate.value = range.startDate;
      form.elements.endDate.value = range.endDate;
    }
  });
  // #/dashboard?periodo=30 abre el dashboard con un periodo rápido ya aplicado.
  const linkedPeriod = routeParams().get('periodo');
  if (PRESETS.some((p) => p.value === linkedPeriod && !['all', 'custom'].includes(p.value))) {
    form.elements.period.value = linkedPeriod;
    form.elements.period.dispatchEvent(new Event('change'));
  }

  ['startDate', 'endDate'].forEach((name) => form.elements[name].addEventListener('input', () => {
    form.elements.period.value = form.elements.startDate.value || form.elements.endDate.value ? 'custom' : 'all';
  }));

  function kpisHtml(d, dist, prev) {
    const pending = (d.totalIncidents ?? 0) - (d.resolvedIncidents ?? 0);
    const period = prev ? `${prev.days} días anteriores` : null;
    const trend = (current, previous, goodWhen) => {
      const delta = prev ? pctChange(current, previous) : null;
      return delta == null ? undefined : { delta, goodWhen, period };
    };
    const p = prev?.data;
    // Series semanales reales para los minigráficos de las tarjetas
    const weeks = d.weeklyTrends ?? [];
    const reported = weeks.map((w) => w.totalReported);
    const resolved = weeks.map((w) => w.totalResolved);
    const backlog = weeks.map((w) => w.totalReported - w.totalResolved);
    const rate = weeks.map((w) => (w.totalReported ? (w.totalResolved / w.totalReported) * 100 : 0));
    return `
      <div class="kpi-grid">
        ${kpiCard({ label: 'Total de incidentes', value: d.totalIncidents, icon: 'i-inbox', spark: reported, trend: trend(d.totalIncidents, p?.totalIncidents, 'down') })}
        ${kpiCard({ label: 'Pendientes', value: pending, icon: 'i-hourglass', tone: 'warning', hint: `${dist.critical} críticos`, spark: backlog,
          trend: trend(pending, p ? p.totalIncidents - p.resolvedIncidents : null, 'down') })}
        ${kpiCard({ label: 'Resueltos o cerrados', value: d.resolvedIncidents, icon: 'i-check-circle', tone: 'success', spark: resolved, trend: trend(d.resolvedIncidents, p?.resolvedIncidents, 'up') })}
        ${kpiCard({ label: 'Porcentaje de resolución', value: d.resolutionRatePercentage, format: 'percent', icon: 'i-percent', tone: 'accent',
          spark: rate, progress: d.resolutionRatePercentage, trend: trend(d.resolutionRatePercentage, p?.resolutionRatePercentage, 'up') })}
        ${kpiCard({ label: 'Tiempo promedio de atención', value: d.averageResolutionTimeHours, format: 'duration', icon: 'i-clock',
          trend: trend(d.averageResolutionTimeHours, p?.averageResolutionTimeHours, 'down'), hint: prev ? '' : 'Desde el registro hasta la resolución' })}
        ${kpiCard({ label: 'Calificación de la IA', value: d.averageAiRatingScore || null, icon: 'i-star', tone: 'accent', hint: 'Promedio sobre 5 estrellas' })}
      </div>`;
  }

  // Resumen del periodo: frases calculadas con los datos reales del backend
  function heroHtml(d, dist, prev) {
    const now = new Date();
    const hour = now.getHours();
    const greeting = hour < 12 ? 'Buenos días' : hour < 19 ? 'Buenas tardes' : 'Buenas noches';
    const today = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long' }).format(now);
    const pending = (d.totalIncidents ?? 0) - (d.resolvedIncidents ?? 0);
    const rate = d.resolutionRatePercentage ?? 0;
    const top = d.topCategories?.[0];
    const delta = prev ? pctChange(d.totalIncidents, prev.data.totalIncidents) : null;
    const fmt = (n, digits = 1) => Number(n).toLocaleString('es-CO', { maximumFractionDigits: digits });
    const circumference = 2 * Math.PI * 52;
    const offset = circumference * (1 - Math.min(Math.max(rate, 0), 100) / 100);

    const chips = [
      dist.critical ? `<li class="hero__chip hero__chip--danger">${icon('i-alert')}${dist.critical} ${dist.critical === 1 ? 'crítico' : 'críticos'} sin resolver</li>` : '',
      top ? `<li class="hero__chip">${icon('i-layers')}Más frecuente: ${escapeHtml(top.categoryName)} (${fmt(top.percentageOfTotal)} %)</li>` : '',
      d.averageResolutionTimeHours ? `<li class="hero__chip">${icon('i-clock')}Atención promedio: ${fmt(d.averageResolutionTimeHours)} h</li>` : '',
      delta != null ? `<li class="hero__chip ${delta > 0 ? 'hero__chip--warning' : 'hero__chip--success'}">${icon(delta > 0 ? 'i-trend-up' : 'i-trend-down')}${delta > 0 ? '+' : ''}${fmt(delta)} % de registros vs. periodo anterior</li>` : '',
    ].join('');

    return `
      <div class="hero__body">
        <p class="hero__greeting">${greeting} · <span>${today}</span></p>
        <h2 class="hero__title">${pending
          ? `<strong>${pending}</strong> ${pending === 1 ? 'incidente requiere' : 'incidentes requieren'} atención`
          : 'Todo al día: no hay incidentes pendientes'}</h2>
        <ul class="hero__chips">${chips}</ul>
        <div class="hero__actions">
          <a class="btn btn--primary" href="#/incidentes?nuevo=1">${icon('i-plus')}Nuevo incidente</a>
          <a class="btn btn--ghost" href="#/asistente">${icon('i-sparkles')}Consultar a la IA</a>
          <a class="btn btn--ghost" href="#/analitica">${icon('i-chart')}Ver analítica</a>
        </div>
      </div>
      <div class="hero__gauge" role="img" aria-label="Porcentaje de resolución: ${fmt(rate)} %">
        <svg viewBox="0 0 120 120" aria-hidden="true">
          <defs>
            <linearGradient id="hero-gauge" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stop-color="#3b82f6"/><stop offset="1" stop-color="#a78bfa"/>
            </linearGradient>
          </defs>
          <circle class="hero__gauge-track" cx="60" cy="60" r="52"/>
          <circle class="hero__gauge-value" cx="60" cy="60" r="52"
                  stroke-dasharray="${circumference.toFixed(1)}" stroke-dashoffset="${offset.toFixed(1)}"/>
        </svg>
        <div class="hero__gauge-label"><strong>${fmt(rate)}<small>%</small></strong><span>resuelto</span></div>
      </div>`;
  }

  function draw(d, incidents, prev) {
    const dist = distributions(incidents);
    $('#d-hero').innerHTML = heroHtml(d, dist, prev);
    kpis.innerHTML = kpisHtml(d, dist, prev);
    animateCounters(kpis);

    const weekly = trendSeries(d.weeklyTrends ?? [], (w) => formatYearWeek(w.yearWeek));
    const monthly = trendSeries(d.monthlyTrends ?? [], (m) => formatYearMonth(m.yearMonth));
    const byCategory = (d.topCategories ?? []).map((c) => ({ label: c.categoryName, value: c.incidentCount }));

    if (charts) {
      charts.weekly.update(weekly);
      charts.monthly.update(monthly);
      charts.times.update(dist.times);
      charts.category.update(byCategory);
      charts.status.update(dist.byStatus);
      charts.priority.update(dist.byPriority);
    } else {
      charts = {
        weekly: lineChart($('#dc-weekly'), { title: 'Evolución semanal de incidentes', labelHeader: 'Semana', integer: true, data: weekly, signal }),
        monthly: lineChart($('#dc-monthly'), { title: 'Evolución mensual de incidentes', labelHeader: 'Mes', integer: true, data: monthly, signal }),
        times: columnChart($('#dc-times'), { title: 'Distribución de tiempos de resolución', labelHeader: 'Rango', integer: true, data: dist.times, signal }),
        category: barChart($('#dc-category'), { title: 'Incidentes por categoría', data: byCategory, signal }),
        status: donutChart($('#dc-status'), { title: 'Incidentes por estado', centerLabel: 'incidentes', data: dist.byStatus, signal }),
        priority: donutChart($('#dc-priority'), { title: 'Incidentes por prioridad', centerLabel: 'incidentes', data: dist.byPriority, signal }),
      };
    }

    const recent = [...incidents]
      .sort((a, b) => (parseDate(b.createdAt) ?? 0) - (parseDate(a.createdAt) ?? 0))
      .slice(0, 6);
    $('#dc-recent').innerHTML = recent.length ? `
      <ul class="recent-list">
        ${recent.map((i) => `
          <li>
            <a class="recent-item" href="#/incidentes?id=${i.id}">
              ${avatar(i.userFullName, 'md')}
              <span class="recent-item__main">
                <span class="cell-title">${escapeHtml(i.title)}</span>
                <span class="cell-muted">#${i.id} · ${escapeHtml(i.categoryName ?? '')} · ${formatDateTime(i.createdAt)}</span>
              </span>
              <span class="recent-item__badges">${badge(PRIORIDAD, i.priority)}${badge(ESTADO_INCIDENTE, i.status)}</span>
            </a>
          </li>`).join('')}
      </ul>` : '<p class="cell-muted">No hay incidentes en este periodo.</p>';
  }

  async function load() {
    const filters = Object.fromEntries(new FormData(form));
    delete filters.period;
    if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) {
      errorEl.textContent = 'La fecha inicial no puede ser posterior a la final.';
      return;
    }
    errorEl.textContent = '';
    scope.textContent = 'Cargando…';

    // Comparación temporal solo con un rango cerrado: el periodo anterior tiene la misma duración.
    const prevRange = previousPeriod(filters.startDate, filters.endDate);
    let dashboard;
    let incidents;
    let previous = null;
    try {
      [dashboard, incidents, previous] = await Promise.all([
        getDashboard(filters, { signal }),
        listIncidents(filters, { signal }),
        prevRange ? getDashboard({ ...filters, ...prevRange }, { signal }) : null,
      ]);
    } catch (error) {
      if (signal.aborted) return;
      scope.textContent = '';
      errorState(kpis, error, load);
      return;
    }
    if (signal.aborted) return;

    const active = Object.values(filters).filter(Boolean).length;
    scope.textContent = `${dashboard.totalIncidents} incidentes${active ? ` · ${active} ${active === 1 ? 'filtro' : 'filtros'}` : ''}`
      + (prevRange ? ' · comparado con el periodo anterior' : '');
    draw(dashboard, incidents, previous ? { data: previous, days: prevRange.days } : null);
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
