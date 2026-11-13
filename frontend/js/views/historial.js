// Historial de consultas (RF08): todas las consultas a la IA con su estado,
// respuesta, fuentes, similitud y calificación.
// Endpoints: GET /api/v1/ai-assistance/history,
//            POST /api/v1/ai-assistance/responses/{id}/ratings

import { pageHeader, icon, escapeHtml } from '../ui.js';
import { errorState } from '../components/states.js';
import { skeletonLines } from '../components/skeleton.js';
import { starsHtml } from '../components/rating.js';
import { createDataTable } from '../components/data-table.js';
import { openModal } from '../components/modal.js';
import { renderAiQuery, responseSummary } from '../components/ai-response.js';
import { getAiHistory } from '../services/ai-assistance.service.js';
import { listUsers, toOptions } from '../services/catalogs.service.js';
import { ESTADO_CONSULTA, badge, estadoConsulta, labelOf, optionsHtml } from '../catalogs.js';
import { formatDate, formatDateTime, parseDate } from '../format.js';
import { routeParams } from '../router.js';

function statsHtml(entries) {
  const ratings = entries.map((q) => q.aiResponse?.rating?.score).filter(Number.isFinite);
  const average = ratings.length ? ratings.reduce((a, b) => a + b, 0) / ratings.length : null;
  const counts = Object.keys(ESTADO_CONSULTA).map((status) => [status, entries.filter((q) => estadoConsulta(q) === status).length]);
  return `
    <dl class="history-stats">
      <div><dt>Consultas</dt><dd>${entries.length}</dd></div>
      ${counts.map(([status, count]) => `<div><dt>${escapeHtml(labelOf(ESTADO_CONSULTA, status))}</dt><dd>${count}</dd></div>`).join('')}
      <div><dt>Calificación promedio</dt><dd>${average != null
        ? `${starsHtml(average)} <span class="cell-muted">${average.toLocaleString('es-CO', { maximumFractionDigits: 1 })} (${ratings.length})</span>`
        : '<span class="cell-muted">Sin calificar</span>'}</dd></div>
    </dl>`;
}

export async function render(container, { route, signal }) {
  container.innerHTML = `
    ${pageHeader(route)}
    <div class="stack stack--tight" id="history">${skeletonLines(4)}</div>`;

  const root = container.querySelector('#history');
  let entries;
  let users;
  try {
    [entries, users] = await Promise.all([getAiHistory({}, { signal }), listUsers({}, { signal })]);
    users = toOptions(users);
  } catch (error) {
    if (signal.aborted) return;
    errorState(root, error, () => render(container, { route, signal }));
    return;
  }
  if (signal.aborted) return;

  // Incidentes que tienen al menos una consulta, para el filtro.
  const incidents = [...new Map(entries.map((q) => [q.incidentId, q.incidentTitle])).entries()]
    .sort((a, b) => b[0] - a[0]);

  root.innerHTML = `
    <form class="card filters" id="h-filters" aria-label="Filtros del historial">
      <div class="filters__grid filters__grid--wide">
        <div class="field"><label for="h-status">Estado</label>
          <select class="input" id="h-status" name="status"><option value="">Todos</option>${optionsHtml(ESTADO_CONSULTA)}</select></div>
        <div class="field"><label for="h-incident">Incidente</label>
          <select class="input" id="h-incident" name="incidentId"><option value="">Todos</option>
            ${incidents.map(([id, title]) => `<option value="${id}">#${id} · ${escapeHtml(title ?? '')}</option>`).join('')}
          </select></div>
      </div>
      <div id="h-stats"></div>
    </form>
    <section class="card" aria-label="Consultas a la IA"><div id="h-table"></div></section>`;

  const form = root.querySelector('#h-filters');
  const stats = root.querySelector('#h-stats');

  const table = createDataTable(root.querySelector('#h-table'), {
    caption: 'Consultas realizadas al asistente IA',
    searchPlaceholder: 'Buscar por incidente, diagnóstico o fuente…',
    emptyMessage: 'No hay consultas con estos filtros.',
    pageSize: 10,
    initialSort: { key: 'queriedAt', dir: 'desc' },
    columns: [
      { key: 'aiQueryId', label: 'ID', align: 'right', render: (q) => `<span class="cell-id">#${q.aiQueryId}</span>`, text: (q) => String(q.aiQueryId) },
      {
        key: 'queriedAt', label: 'Fecha', render: (q) => `<span class="cell-muted" title="${formatDateTime(q.queriedAt)}">${formatDate(q.queriedAt)}</span>`,
        text: (q) => formatDateTime(q.queriedAt), sortValue: (q) => parseDate(q.queriedAt),
      },
      {
        key: 'incidentTitle', label: 'Incidente',
        render: (q) => `<span class="cell-title cell-title--narrow" title="${escapeHtml(q.incidentTitle)}">#${q.incidentId} · ${escapeHtml(q.incidentTitle)}</span>`,
        text: (q) => `${q.incidentId} ${q.incidentTitle} ${q.aiResponse?.suggestedDiagnosis ?? ''} ${(q.aiResponse?.sources ?? []).map((s) => s.sourceTitle).join(' ')}`,
      },
      {
        key: 'status', label: 'Estado', render: (q) => badge(ESTADO_CONSULTA, estadoConsulta(q)),
        text: (q) => labelOf(ESTADO_CONSULTA, estadoConsulta(q)), sortValue: (q) => Object.keys(ESTADO_CONSULTA).indexOf(estadoConsulta(q)),
      },
      {
        key: 'summary', label: 'Resumen de la respuesta', sortable: false,
        render: (q) => `<span class="cell-muted cell-clip" title="${escapeHtml(responseSummary(q))}">${escapeHtml(responseSummary(q))}</span>`,
        text: (q) => responseSummary(q),
      },
      {
        key: 'sources', label: 'Fuentes', align: 'right',
        render: (q) => `<span class="cell-muted">${q.aiResponse ? q.aiResponse.sources.length : '—'}</span>`,
        text: () => '', sortValue: (q) => q.aiResponse?.sources.length ?? -1,
      },
      {
        key: 'rating', label: 'Calificación',
        render: (q) => (q.aiResponse?.rating ? starsHtml(q.aiResponse.rating.score)
          : q.aiResponse ? '<span class="badge badge--neutral">Pendiente</span>' : '<span class="cell-muted">—</span>'),
        text: () => '', sortValue: (q) => q.aiResponse?.rating?.score ?? -1,
      },
    ],
    onRowClick: (query) => openDetail(query),
  });

  const filtered = () => {
    const { status, incidentId } = Object.fromEntries(new FormData(form));
    return entries.filter((q) => (!status || estadoConsulta(q) === status) && (!incidentId || String(q.incidentId) === incidentId));
  };

  const refresh = () => {
    const rows = filtered();
    table.setRows(rows);
    stats.innerHTML = statsHtml(rows);
  };

  form.addEventListener('change', refresh);
  form.addEventListener('submit', (event) => event.preventDefault());

  function openDetail(query) {
    const modal = openModal({ title: `Consulta #${query.aiQueryId}`, size: 'lg', body: '<div data-query></div>' });
    const slot = modal.body.querySelector('[data-query]');
    slot.insertAdjacentHTML('beforebegin', `
      <p class="detail-links detail-links--top">
        <a class="btn btn--ghost" href="#/incidentes?id=${query.incidentId}">${icon('i-incident')}Incidente #${query.incidentId}</a>
        <a class="btn btn--ghost" href="#/asistente?incidente=${query.incidentId}">${icon('i-sparkles')}Nueva consulta</a>
      </p>`);
    modal.body.querySelectorAll('.detail-links a').forEach((a) => a.addEventListener('click', () => modal.close()));
    renderAiQuery(slot, query, {
      users,
      heading: query.incidentTitle,
      onRated: (rating) => {
        query.aiResponse.rating = rating;
        refresh();
      },
    });
  }

  // #/historial?incidente=12 llega desde el detalle de un incidente.
  const preselected = routeParams().get('incidente');
  if (preselected) form.elements.incidentId.value = incidents.some(([id]) => String(id) === preselected) ? preselected : '';
  refresh();
  if (preselected && !form.elements.incidentId.value) {
    table.setRows([]);
    stats.innerHTML = `<p class="cell-muted">El incidente #${escapeHtml(preselected)} todavía no tiene consultas a la IA.
      <a href="#/asistente?incidente=${escapeHtml(preselected)}">Consultar ahora</a></p>`;
  }
}
