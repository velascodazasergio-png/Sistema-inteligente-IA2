// Asistente IA (RF05): pide orientación técnica para un incidente.
// El backend registra la consulta y la envía a n8n (RAG con Qdrant + LLM).
// Cuando n8n devuelve la respuesta, el backend la guarda; aquí se consulta
// el historial del incidente hasta que aparezca.
// Endpoints: POST /api/v1/ai-assistance/request, GET /api/v1/ai-assistance/incidents/{id}/history

import { pageHeader, icon, escapeHtml, toast, setBusy } from '../ui.js';
import { errorState, emptyHtml } from '../components/states.js';
import { skeletonLines } from '../components/skeleton.js';
import { renderAiQuery } from '../components/ai-response.js';
import { listIncidents, catalogsFromIncidents } from '../services/incidents.service.js';
import { requestAssistance, getIncidentHistory } from '../services/ai-assistance.service.js';
import { ESTADO_INCIDENTE, PRIORIDAD, badge } from '../catalogs.js';
import { formatDateTime } from '../format.js';
import { routeParams } from '../router.js';

const POLL_INTERVAL_MS = 3000;
const POLL_LIMIT_MS = 90000;

const wait = (ms, signal) => new Promise((resolve, reject) => {
  const timer = setTimeout(resolve, ms);
  signal.addEventListener('abort', () => { clearTimeout(timer); reject(new DOMException('Aborted', 'AbortError')); }, { once: true });
});

const latestFirst = (history) => [...history].sort((a, b) => b.aiQueryId - a.aiQueryId);

export async function render(container, { route, signal }) {
  container.innerHTML = `
    ${pageHeader({ eyebrow: route.rf, title: route.title, description: route.description })}
    <div class="assistant" id="assistant">${skeletonLines(4)}</div>`;

  const root = container.querySelector('#assistant');
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
  // Regla del backend: no se puede consultar a la IA sobre un incidente cerrado.
  const available = incidents.filter((i) => i.status !== 'CLOSED').sort((a, b) => b.id - a.id);

  root.innerHTML = `
    <form class="card assistant__form" id="ai-form" novalidate>
      <h2 class="card__title">Nueva consulta</h2>
      <p class="card__subtitle">El asistente responde solo con los documentos de la base de conocimiento que encuentre relevantes.</p>

      <div class="field">
        <label for="ai-incident">Incidente</label>
        <select class="input" id="ai-incident" name="incidentId" aria-describedby="ai-incident-hint ai-incident-error">
          <option value="">Selecciona un incidente…</option>
          ${available.map((i) => `<option value="${i.id}">#${i.id} · ${escapeHtml(i.title)}</option>`).join('')}
        </select>
        <p class="field__hint" id="ai-incident-hint">${available.length} incidentes abiertos. Los cerrados no admiten consultas.</p>
        <p class="field__error" id="ai-incident-error" aria-live="polite"></p>
      </div>

      <div id="ai-summary"></div>

      <div class="field">
        <label for="ai-context">Contexto adicional (opcional)</label>
        <textarea class="input input--area" id="ai-context" name="context" rows="3" maxlength="500"
          placeholder="Ej.: empezó después de la actualización del firewall de ayer"></textarea>
        <p class="field__hint"><span data-count>0</span>/500 caracteres</p>
      </div>

      <div class="form__actions">
        <button class="btn btn--primary" type="submit">${icon('i-sparkles')}Consultar al asistente</button>
        <a class="btn btn--ghost" href="#/incidentes">${icon('i-plus')}Registrar otro incidente</a>
      </div>
    </form>

    <section class="assistant__result" aria-live="polite" aria-label="Respuesta del asistente" id="ai-result">
      ${emptyHtml('Elige un incidente', 'Aquí verás la respuesta del asistente: diagnóstico, pasos recomendados y las fuentes que la respaldan.', 'i-sparkles')}
    </section>`;

  const form = root.querySelector('#ai-form');
  const select = form.elements.incidentId;
  const summary = root.querySelector('#ai-summary');
  const result = root.querySelector('#ai-result');
  const submit = form.querySelector('[type="submit"]');
  const errorEl = root.querySelector('#ai-incident-error');
  let busy = false;

  const current = () => available.find((i) => String(i.id) === select.value);

  const showQuery = (query, heading) => renderAiQuery(result, query, { users, heading });

  async function showLatest(incident) {
    result.innerHTML = skeletonLines(4);
    try {
      const history = latestFirst(await getIncidentHistory(incident.id, { signal }));
      if (signal.aborted || current() !== incident) return;
      if (history.length) showQuery(history[0], 'Última consulta de este incidente');
      else result.innerHTML = emptyHtml('Sin consultas previas', 'Este incidente todavía no se ha consultado con el asistente.', 'i-sparkles');
    } catch (error) {
      if (signal.aborted) return;
      result.innerHTML = emptyHtml('No se pudo cargar el historial', error.message, 'i-alert');
    }
  }

  function selectIncident() {
    const incident = current();
    errorEl.textContent = '';
    select.removeAttribute('aria-invalid');
    if (!incident) {
      summary.innerHTML = '';
      return;
    }
    summary.innerHTML = `
      <div class="incident-summary">
        <div class="cluster">${badge(PRIORIDAD, incident.priority)}${badge(ESTADO_INCIDENTE, incident.status)}
          <span class="cell-muted">${escapeHtml(incident.categoryName)} · ${formatDateTime(incident.createdAt)}</span></div>
        <p class="detail-text">${escapeHtml(incident.description)}</p>
      </div>`;
    if (!busy) showLatest(incident);
  }

  select.addEventListener('change', selectIncident);
  form.elements.context.addEventListener('input', (e) => { form.querySelector('[data-count]').textContent = e.target.value.length; });

  // Consulta el historial hasta que la respuesta llegue, falle o se agote el tiempo.
  async function waitForResponse(incident, aiQueryId) {
    const started = Date.now();
    while (Date.now() - started < POLL_LIMIT_MS) {
      await wait(POLL_INTERVAL_MS, signal);
      const history = await getIncidentHistory(incident.id, { signal });
      const query = history.find((q) => q.aiQueryId === aiQueryId);
      if (query && (query.aiResponse || query.queryStatus !== 'PENDING')) return query;
    }
    return null;
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const incident = current();
    if (!incident) {
      errorEl.textContent = 'Elige el incidente que quieres consultar.';
      select.setAttribute('aria-invalid', 'true');
      select.focus();
      return;
    }

    busy = true;
    setBusy(submit, true, 'Consultando…');
    select.disabled = true;
    try {
      const sent = await requestAssistance(incident, form.elements.context.value, { signal });
      incident.status = 'IN_AI_ANALYSIS';
      selectIncident();

      const pending = { aiQueryId: sent.aiQueryId, incidentId: incident.id, queryStatus: sent.queryStatus, queriedAt: sent.timestamp };
      showQuery(pending, 'Nueva consulta');

      if (sent.queryStatus === 'COMMUNICATION_ERROR') {
        toast('El backend registró la consulta, pero no pudo comunicarse con n8n.', { type: 'error' });
      } else if (sent.queryStatus === 'PENDING') {
        const answered = await waitForResponse(incident, sent.aiQueryId);
        if (answered) {
          showQuery(answered, 'Nueva consulta');
          toast('El asistente respondió.', { type: 'success' });
        } else {
          result.querySelector('.ai-wait')?.replaceChildren(
            `La respuesta tarda más de lo normal. Quedará guardada en el historial del incidente #${incident.id}.`,
          );
          toast(`Sin respuesta todavía. Revisa el historial del incidente #${incident.id} más tarde.`, { type: 'warning' });
        }
      }
    } catch (error) {
      if (signal.aborted) return;
      toast(error.message, { type: 'error' });
    } finally {
      busy = false;
      if (!signal.aborted) {
        setBusy(submit, false);
        select.disabled = false;
      }
    }
  });

  // #/asistente?incidente=12 llega desde el detalle de un incidente.
  const preselected = routeParams().get('incidente');
  if (preselected) {
    select.value = preselected;
    if (current()) selectIncident();
    else toast(`El incidente #${preselected} no existe o está cerrado.`, { type: 'warning' });
  }
}
