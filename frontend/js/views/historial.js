// Historial de consultas (RF08): consultas a la IA de un incidente, con su
// respuesta, fuentes, similitud y calificación.
// Endpoints: GET /api/v1/ai-assistance/incidents/{id}/history,
//            POST /api/v1/ai-assistance/responses/{id}/ratings
// El backend aún no tiene un listado global de consultas: se elige el incidente.

import { pageHeader, icon, escapeHtml } from '../ui.js';
import { errorState, emptyHtml } from '../components/states.js';
import { skeletonLines } from '../components/skeleton.js';
import { starsHtml } from '../components/rating.js';
import { renderAiQuery } from '../components/ai-response.js';
import { listIncidents, catalogsFromIncidents } from '../services/incidents.service.js';
import { getIncidentHistory } from '../services/ai-assistance.service.js';
import { ESTADO_INCIDENTE, PRIORIDAD, badge, estadoConsulta, labelOf, ESTADO_CONSULTA } from '../catalogs.js';
import { routeParams } from '../router.js';

function statsHtml(history) {
  const ratings = history.map((q) => q.aiResponse?.rating?.score).filter(Number.isFinite);
  const average = ratings.length ? ratings.reduce((a, b) => a + b, 0) / ratings.length : null;
  const byStatus = history.reduce((acc, q) => {
    const status = estadoConsulta(q);
    acc[status] = (acc[status] ?? 0) + 1;
    return acc;
  }, {});
  return `
    <dl class="history-stats">
      <div><dt>Consultas</dt><dd>${history.length}</dd></div>
      ${Object.entries(byStatus).map(([status, count]) => `<div><dt>${escapeHtml(labelOf(ESTADO_CONSULTA, status))}</dt><dd>${count}</dd></div>`).join('')}
      <div><dt>Calificación promedio</dt><dd>${average != null ? `${starsHtml(average)} <span class="cell-muted">${average.toLocaleString('es-CO', { maximumFractionDigits: 1 })} (${ratings.length})</span>` : '<span class="cell-muted">Sin calificar</span>'}</dd></div>
    </dl>`;
}

export async function render(container, { route, signal }) {
  container.innerHTML = `
    ${pageHeader({ eyebrow: route.rf, title: route.title, description: route.description })}
    <div class="stack stack--tight" id="history">${skeletonLines(4)}</div>`;

  const root = container.querySelector('#history');
  let incidents;
  try {
    incidents = await listIncidents({}, { signal });
  } catch (error) {
    if (signal.aborted) return;
    errorState(root, error, () => render(container, { route, signal }));
    return;
  }
  if (signal.aborted) return;

  const { users } = catalogsFromIncidents(incidents);
  const sorted = [...incidents].sort((a, b) => b.id - a.id);

  root.innerHTML = `
    <div class="card">
      <div class="field">
        <label for="h-incident">Incidente</label>
        <select class="input" id="h-incident">
          <option value="">Selecciona un incidente…</option>
          ${sorted.map((i) => `<option value="${i.id}">#${i.id} · ${escapeHtml(i.title)}</option>`).join('')}
        </select>
        <p class="field__hint">El backend guarda el historial por incidente. Un listado global de consultas está pendiente.</p>
      </div>
      <div id="h-summary"></div>
    </div>
    <div id="h-list" aria-live="polite">
      ${emptyHtml('Elige un incidente', 'Verás todas las consultas que se hicieron al asistente, sus respuestas, las fuentes usadas y su calificación.', 'i-history')}
    </div>`;

  const select = root.querySelector('#h-incident');
  const summary = root.querySelector('#h-summary');
  const list = root.querySelector('#h-list');
  let requestId = 0;

  async function show() {
    const incident = incidents.find((i) => String(i.id) === select.value);
    const myRequest = ++requestId;
    if (!incident) {
      summary.innerHTML = '';
      list.innerHTML = emptyHtml('Elige un incidente', '', 'i-history');
      return;
    }
    history.replaceState(null, '', `#/historial?incidente=${incident.id}`);

    list.innerHTML = skeletonLines(5);
    let entries;
    try {
      entries = await getIncidentHistory(incident.id, { signal });
    } catch (error) {
      if (signal.aborted || myRequest !== requestId) return;
      errorState(list, error, show);
      return;
    }
    if (signal.aborted || myRequest !== requestId) return;

    entries.sort((a, b) => b.aiQueryId - a.aiQueryId);
    summary.innerHTML = `
      <div class="incident-summary">
        <div class="cluster">${badge(PRIORIDAD, incident.priority)}${badge(ESTADO_INCIDENTE, incident.status)}
          <span class="cell-muted">${escapeHtml(incident.categoryName)}</span></div>
        ${entries.length ? statsHtml(entries) : ''}
      </div>`;

    if (!entries.length) {
      list.innerHTML = `
        <div class="card">${emptyHtml('Sin consultas', 'Este incidente todavía no se ha consultado con el asistente.', 'i-sparkles')}
          ${incident.status !== 'CLOSED' ? `<div class="cluster cluster--center"><a class="btn btn--primary" href="#/asistente?incidente=${incident.id}">${icon('i-sparkles')}Consultar ahora</a></div>` : ''}
        </div>`;
      return;
    }

    list.innerHTML = `<ol class="timeline">${entries.map((q) => `<li class="timeline__item" data-query="${q.aiQueryId}"></li>`).join('')}</ol>`;
    entries.forEach((query) => {
      renderAiQuery(list.querySelector(`[data-query="${query.aiQueryId}"]`), query, {
        users,
        onRated: (rating) => {
          query.aiResponse.rating = rating;
          summary.querySelector('.history-stats')?.replaceWith(
            Object.assign(document.createElement('div'), { innerHTML: statsHtml(entries) }).firstElementChild,
          );
        },
      });
    });
  }

  select.addEventListener('change', show);

  const preselected = routeParams().get('incidente');
  if (preselected && incidents.some((i) => String(i.id) === preselected)) {
    select.value = preselected;
    show();
  }
}
