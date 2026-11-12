// Asistente IA (RF05): orientación técnica para un incidente.
// El backend registra la consulta y la envía a n8n (RAG con Qdrant + LLM).
// Cuando n8n devuelve la respuesta, el backend la guarda; aquí se consulta
// el historial del incidente hasta que aparezca.
// Endpoints: POST /api/v1/ai-assistance/request, GET /api/v1/ai-assistance/incidents/{id}/history

import { pageHeader, icon, escapeHtml, toast, setBusy } from '../ui.js';
import { errorState, emptyHtml } from '../components/states.js';
import { skeletonLines } from '../components/skeleton.js';
import { renderAiQuery } from '../components/ai-response.js';
import { listIncidents } from '../services/incidents.service.js';
import { listUsers, toOptions } from '../services/catalogs.service.js';
import { requestAssistance, getIncidentHistory, MIN_DESCRIPTION } from '../services/ai-assistance.service.js';
import { ESTADO_INCIDENTE, PRIORIDAD, badge } from '../catalogs.js';
import { formatDateTime } from '../format.js';
import { routeParams } from '../router.js';

const POLL_INTERVAL_MS = 3000;
const POLL_LIMIT_MS = 90000;

const wait = (ms, signal) => new Promise((resolve, reject) => {
  const timer = setTimeout(resolve, ms);
  signal.addEventListener('abort', () => { clearTimeout(timer); reject(new DOMException('Aborted', 'AbortError')); }, { once: true });
});

// El backend guarda el prompt como "[cat | Priority] título - descripción. Additional Context: …".
const contextOf = (prompt) => {
  const match = /Additional Context:\s*([\s\S]*)$/.exec(prompt ?? '');
  const text = match?.[1]?.trim();
  return text && text !== 'None' ? text : '';
};

const userTurnHtml = (incident, context, date) => `
  <div class="chat-turn chat-turn--user">
    <div class="chat-bubble">
      <p class="chat-bubble__meta">Consulta sobre el incidente #${incident.id}${date ? ` · ${formatDateTime(date)}` : ''}</p>
      <p>${escapeHtml(context || 'Analiza este incidente y propón una solución con la base de conocimiento.')}</p>
    </div>
    <span class="chat-avatar" aria-hidden="true">Tú</span>
  </div>`;

const EMPTY_THREAD = () => emptyHtml('Elige un incidente', 'Verás aquí cada consulta con su diagnóstico, solución, pasos y las fuentes que la respaldan.', 'i-sparkles');

export async function render(container, { route, signal }) {
  container.innerHTML = `
    ${pageHeader(route)}
    <div class="assistant" id="assistant">${skeletonLines(4)}</div>`;

  const root = container.querySelector('#assistant');
  let incidents;
  let users;
  try {
    [incidents, users] = await Promise.all([listIncidents({}, { signal }), listUsers({}, { signal })]);
    users = toOptions(users);
  } catch (error) {
    if (signal.aborted) return;
    errorState(root, error, () => render(container, { route, signal }));
    return;
  }
  if (signal.aborted) return;

  // Regla del backend: no se puede consultar a la IA sobre un incidente cerrado.
  const available = incidents.filter((i) => i.status !== 'CLOSED').sort((a, b) => b.id - a.id);

  root.innerHTML = `
    <form class="card assistant__form" id="ai-form" novalidate>
      <div class="assistant__brand">
        <span class="ai-avatar ai-avatar--lg" aria-hidden="true">${icon('i-sparkles')}</span>
        <div>
          <h2 class="card__title">Nueva consulta</h2>
          <p class="card__subtitle">Responde solo con artículos activos y verificados de la base de conocimiento.</p>
        </div>
      </div>

      <div class="field">
        <label for="ai-incident">Incidente</label>
        <select class="input" id="ai-incident" name="incidentId" aria-describedby="ai-incident-hint ai-incident-error">
          <option value="">Selecciona un incidente…</option>
          ${available.map((i) => `<option value="${i.id}">#${i.id} · ${escapeHtml(i.title)}</option>`).join('')}
        </select>
        <p class="field__hint" id="ai-incident-hint">${available.length} incidentes disponibles. Los cerrados no admiten consultas.</p>
        <p class="field__error" id="ai-incident-error" aria-live="polite"></p>
      </div>

      <div id="ai-summary"></div>

      <div class="field">
        <label for="ai-context">Contexto adicional <span class="cell-muted">(opcional)</span></label>
        <textarea class="input input--area" id="ai-context" name="context" rows="3" maxlength="500"
          placeholder="Ej.: empezó después de la actualización del firewall de ayer"></textarea>
        <p class="field__hint"><span data-count>0</span>/500 caracteres · Ctrl + Enter para enviar</p>
      </div>

      <div class="form__actions">
        <button class="btn btn--primary" type="submit">${icon('i-send')}Consultar al asistente</button>
        <a class="btn btn--ghost" href="#/incidentes">${icon('i-plus')}Registrar incidente</a>
      </div>
    </form>

    <section class="card chat" aria-labelledby="chat-title">
      <header class="chat__head">
        <span class="ai-avatar" aria-hidden="true">${icon('i-sparkles')}</span>
        <div>
          <h2 class="card__title" id="chat-title">Conversación con el asistente</h2>
          <p class="card__subtitle" id="chat-subtitle">Elige un incidente para ver sus consultas.</p>
        </div>
      </header>
      <div class="chat__thread" id="ai-thread" aria-live="polite">${EMPTY_THREAD()}</div>
    </section>`;

  const form = root.querySelector('#ai-form');
  const select = form.elements.incidentId;
  const contextField = form.elements.context;
  const summary = root.querySelector('#ai-summary');
  const thread = root.querySelector('#ai-thread');
  const subtitle = root.querySelector('#chat-subtitle');
  const submit = form.querySelector('[type="submit"]');
  const errorEl = root.querySelector('#ai-incident-error');
  let busy = false;

  const current = () => available.find((i) => String(i.id) === select.value);

  // Un turno = pregunta del usuario + tarjeta de la IA. Devuelve el contenedor de la tarjeta.
  function appendTurn(incident, query) {
    const turn = document.createElement('div');
    turn.className = 'chat-exchange';
    turn.innerHTML = `${userTurnHtml(incident, contextOf(query.sentPrompt) || query.context, query.queriedAt)}<div class="chat-turn chat-turn--ai" data-ai></div>`;
    thread.append(turn);
    const slot = turn.querySelector('[data-ai]');
    renderAiQuery(slot, query, { users });
    return slot;
  }

  function renderSummary(incident) {
    const short = incident.description.trim().length < MIN_DESCRIPTION;
    summary.innerHTML = `
      <div class="incident-summary">
        <div class="cluster">${badge(PRIORIDAD, incident.priority)}${badge(ESTADO_INCIDENTE, incident.status)}
          <span class="cell-muted">${escapeHtml(incident.categoryName)} · ${formatDateTime(incident.createdAt)}</span></div>
        <p class="detail-text">${escapeHtml(incident.description)}</p>
        ${short ? `<p class="field__error">La descripción es muy corta (mínimo ${MIN_DESCRIPTION} caracteres): amplíala antes de consultar.</p>` : ''}
      </div>`;
  }

  async function loadThread(incident) {
    thread.innerHTML = skeletonLines(4);
    subtitle.textContent = `Incidente #${incident.id} · ${incident.title}`;
    try {
      const history = (await getIncidentHistory(incident.id, { signal })).sort((a, b) => a.aiQueryId - b.aiQueryId);
      if (signal.aborted || current() !== incident) return;
      thread.replaceChildren();
      if (!history.length) {
        thread.innerHTML = emptyHtml('Sin consultas previas', 'Escribe un contexto opcional y envía la primera consulta.', 'i-sparkles');
        return;
      }
      history.forEach((q) => appendTurn(incident, q));
      // Se muestra el inicio de la última consulta, no el final, para leer la respuesta desde el diagnóstico
      const last = thread.lastElementChild;
      if (last) thread.scrollTop = last.offsetTop - thread.offsetTop;
    } catch (error) {
      if (signal.aborted) return;
      thread.innerHTML = emptyHtml('No se pudo cargar la conversación', error.message, 'i-alert');
    }
  }

  function selectIncident() {
    const incident = current();
    errorEl.textContent = '';
    select.removeAttribute('aria-invalid');
    if (!incident) {
      summary.innerHTML = '';
      subtitle.textContent = 'Elige un incidente para ver sus consultas.';
      thread.innerHTML = EMPTY_THREAD();
      return;
    }
    renderSummary(incident);
    if (!busy) loadThread(incident);
  }

  select.addEventListener('change', selectIncident);
  contextField.addEventListener('input', () => { form.querySelector('[data-count]').textContent = contextField.value.length; });
  contextField.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) form.requestSubmit();
  });

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
    if (busy) return;
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
    const context = contextField.value.trim();
    try {
      const sent = await requestAssistance(incident, context, { signal });
      incident.status = 'IN_AI_ANALYSIS';
      renderSummary(incident);
      contextField.value = '';
      form.querySelector('[data-count]').textContent = '0';
      if (!thread.querySelector('.chat-exchange')) thread.replaceChildren();

      const pending = { aiQueryId: sent.aiQueryId, incidentId: incident.id, queryStatus: sent.queryStatus, queriedAt: sent.timestamp, context };
      if (sent.queryStatus === 'COMMUNICATION_ERROR') {
        pending.failureMessage = 'El backend registró la consulta, pero no pudo comunicarse con el servicio de IA (n8n).';
      }
      const slot = appendTurn(incident, pending);
      slot.scrollIntoView({ block: 'nearest' });

      if (sent.queryStatus === 'COMMUNICATION_ERROR') {
        toast('No se pudo contactar al servicio de IA. La consulta quedó registrada con error.', { type: 'error' });
      } else if (sent.queryStatus === 'PENDING') {
        const answered = await waitForResponse(incident, sent.aiQueryId);
        if (answered) {
          renderAiQuery(slot, { ...answered, context }, { users });
          const ok = answered.aiResponse && answered.queryStatus === 'COMPLETED';
          toast(ok ? 'El asistente respondió.' : 'La consulta terminó sin una solución. Revisa el detalle.', { type: ok ? 'success' : 'warning' });
        } else {
          const title = slot.querySelector('.ai-search__title');
          if (title) title.textContent = `La respuesta tarda más de lo normal. Quedará guardada en el historial del incidente #${incident.id}.`;
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
