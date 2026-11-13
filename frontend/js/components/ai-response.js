// Tarjeta de una consulta a la IA (AiQueryHistoryResponseDTO): estado,
// diagnóstico, solución, pasos, fuentes con similitud y calificación (RF05 / RF08).
// La usan el Asistente y el Historial. Solo muestra lo que devolvió el backend:
// si no hay respuesta, no se simula ninguna.

import { icon, escapeHtml, toast, setBusy } from '../ui.js';
import { createStarRating, starsHtml } from './rating.js';
import { ESTADO_CONSULTA, badge, estadoConsulta } from '../catalogs.js';
import { formatDateTime, formatRatio } from '../format.js';
import { rateResponse } from '../services/ai-assistance.service.js';
import { showServerFieldErrors } from './form-errors.js';

// Mensajes del backend por código de fallo, en el idioma de la interfaz.
const FAILURE_MESSAGES = {
  N8N_UNREACHABLE: 'El servicio de IA (n8n) no aceptó la solicitud o no respondió a tiempo.',
};
const failureText = (query) => FAILURE_MESSAGES[query.failureCode] ?? query.failureMessage;

const decimal = new Intl.NumberFormat('es-CO', { minimumFractionDigits: 2, maximumFractionDigits: 4 });

// Solo enlaces http(s): una URL "javascript:" de una fuente no debe ejecutarse.
const safeUrl = (value) => {
  try {
    const url = new URL(value);
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null;
  } catch {
    return null;
  }
};

/**
 * Separa el texto del flujo RAG en secciones. n8n envía:
 *   suggestedDiagnosis: "Solución sugerida: …\n\nExplicación del problema: …"
 *   solutionSteps:      "1. …\n2. …\n\nAdvertencias:\n- …"
 * Si el texto no sigue ese formato se muestra completo como diagnóstico.
 */
export function parseResponse(response) {
  const diagnosis = response?.suggestedDiagnosis?.trim() ?? '';
  const match = /^Soluci[oó]n sugerida:\s*([\s\S]*?)(?:\n\s*\nExplicaci[oó]n del problema:\s*([\s\S]*))?$/i.exec(diagnosis);
  const stepsText = response?.solutionSteps?.trim() ?? '';
  const [stepsPart, warningsPart = ''] = /^Advertencias:/i.test(stepsText)
    ? ['', stepsText.replace(/^Advertencias:\s*/i, '')]
    : stepsText.split(/\n\s*\nAdvertencias:\s*\n?/i);
  return {
    explanation: match ? (match[2]?.trim() ?? '') : diagnosis,
    solution: match ? match[1].trim() : '',
    steps: splitSteps(stepsPart),
    warnings: warningsPart.split('\n').map((w) => w.replace(/^\s*-\s*/, '').trim()).filter(Boolean),
  };
}

// Pasos: uno por línea; si todo viene en una línea, se separa por "1. … 2. …".
// Solo números de 1 o 2 dígitos marcan un paso, para no cortar "evento 4740." o "versión 2.3".
function splitSteps(text) {
  const parts = /\n/.test(text.trim()) ? text.split(/\n+/) : text.split(/\s+(?=\d{1,2}[.)]\s)/);
  return parts.map((s) => s.replace(/^\s*\d{1,2}[.)]\s+/, '').trim()).filter(Boolean);
}

const meter = (value, label) => `
  <span class="meter" role="img" aria-label="${escapeHtml(label)}: ${formatRatio(value)}">
    <span class="meter__bar"><span style="width: ${Math.max(0, Math.min(1, value ?? 0)) * 100}%"></span></span>
    <span class="meter__value">${formatRatio(value)}</span>
  </span>`;

function sourcesHtml(sources) {
  const sorted = [...sources].sort((a, b) => (b.similarityScore ?? 0) - (a.similarityScore ?? 0));
  return `
    <ul class="ai-sources">
      ${sorted.map((s, i) => {
        const url = safeUrl(s.referenceUrl);
        return `
          <li>
            <details class="ai-source" ${i === 0 ? 'open' : ''}>
              <summary class="ai-source__head">
                <span class="ai-source__title">${icon('i-book')}${escapeHtml(s.sourceTitle ?? 'Fuente sin título')}</span>
                ${meter(s.similarityScore, 'Similitud')}
                ${icon('i-chevron-down', 'ai-source__chevron')}
              </summary>
              <div class="ai-source__body">
                <p class="ai-source__score">Similitud: <strong>${s.similarityScore != null ? decimal.format(s.similarityScore) : '—'}</strong>
                  ${s.knowledgeArticleId ? ` · artículo #${s.knowledgeArticleId}` : ''}</p>
                ${s.citedSnippet ? `<blockquote class="ai-source__snippet">${escapeHtml(s.citedSnippet)}</blockquote>` : ''}
                ${url ? `<a class="ai-source__link" href="${escapeHtml(url)}" target="_blank" rel="noopener noreferrer">${escapeHtml(url)}${icon('i-external')}</a>` : ''}
              </div>
            </details>
          </li>`;
      }).join('')}
    </ul>`;
}

function ratingSummaryHtml(rating) {
  return `
    <div class="ai-rating ai-rating--done">
      <div class="cluster">
        ${starsHtml(rating.score)}
        <span class="badge badge--${rating.helpful ? 'success' : 'neutral'}">${rating.helpful ? 'Fue útil' : 'No fue útil'}</span>
        <span class="cell-muted">${formatDateTime(rating.ratedAt)}</span>
      </div>
      ${rating.feedbackComment ? `<p class="detail-text">“${escapeHtml(rating.feedbackComment)}”</p>` : ''}
    </div>`;
}

// Animación mientras n8n busca en la base de conocimiento y genera la respuesta.
// Describe el proceso; no inventa progreso: termina cuando llega la respuesta real.
export const searchingHtml = () => `
  <div class="ai-search" role="status">
    <div class="ai-search__orb" aria-hidden="true"><span></span><span></span><span></span>${icon('i-sparkles')}</div>
    <div>
      <p class="ai-search__title">Analizando el incidente…</p>
      <ol class="ai-search__steps">
        <li>Buscando documentos relevantes en la base de conocimiento</li>
        <li>Validando que las fuentes estén activas y verificadas</li>
        <li>Redactando la orientación con las fuentes encontradas</li>
      </ol>
    </div>
  </div>`;

let uid = 0;

// Formulario de calificación. users: [{id, name}] para elegir al evaluador.
function mountRatingForm(slot, aiResponseId, users, onRated) {
  const id = `rate-${++uid}`;
  slot.innerHTML = `
    <form class="ai-rating" novalidate>
      <h4 class="detail-label">¿Qué tan útil fue esta respuesta?</h4>
      <div data-stars></div>
      <p class="field__error" id="${id}-stars-error" aria-live="polite"></p>
      <div class="form-row">
        <div class="field">
          <label for="${id}-user">Evaluado por</label>
          <select class="input" id="${id}-user" name="evaluatorUserId">
            <option value="">Selecciona…</option>
            ${users.map((u) => `<option value="${u.id}">${escapeHtml(u.name)}</option>`).join('')}
          </select>
          <p class="field__error" id="${id}-user-error" aria-live="polite"></p>
        </div>
        <fieldset class="field segmented">
          <legend>¿Resolvió el problema?</legend>
          <label><input type="radio" name="helpful" value="true" checked> Sí</label>
          <label><input type="radio" name="helpful" value="false"> No</label>
        </fieldset>
      </div>
      <div class="field">
        <label for="${id}-comment">Comentario (opcional)</label>
        <textarea class="input input--area" id="${id}-comment" name="feedbackComment" rows="2" maxlength="500"
          placeholder="¿Qué funcionó o qué faltó?" aria-describedby="${id}-comment-error"></textarea>
        <p class="field__error" id="${id}-comment-error" aria-live="polite"></p>
      </div>
      <button class="btn btn--primary" type="submit">${icon('i-star')}Enviar calificación</button>
    </form>`;

  const form = slot.querySelector('form');
  const stars = createStarRating(form.querySelector('[data-stars]'), {
    label: 'Calificación de la respuesta',
    onChange: () => { form.querySelector(`#${id}-stars-error`).textContent = ''; },
  });

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(form));
    const starsError = stars.value ? '' : 'Elige de 1 a 5 estrellas.';
    const userError = data.evaluatorUserId ? '' : 'Elige quién califica.';
    form.querySelector(`#${id}-stars-error`).textContent = starsError;
    form.querySelector(`#${id}-user-error`).textContent = userError;
    form.elements.evaluatorUserId.setAttribute('aria-invalid', String(Boolean(userError)));
    if (starsError || userError) return;

    const button = form.querySelector('[type="submit"]');
    setBusy(button, true, 'Enviando…');
    stars.setDisabled(true);
    try {
      const rating = await rateResponse(aiResponseId, {
        evaluatorUserId: data.evaluatorUserId,
        score: stars.value,
        helpful: data.helpful === 'true',
        feedbackComment: data.feedbackComment.trim(),
      });
      slot.innerHTML = ratingSummaryHtml(rating);
      toast('Gracias: la calificación quedó registrada.', { type: 'success' });
      onRated?.(rating);
    } catch (error) {
      setBusy(button, false);
      stars.setDisabled(false);
      const errorIds = { score: `${id}-stars-error`, evaluatorUserId: `${id}-user-error`, feedbackComment: `${id}-comment-error` };
      const shown = showServerFieldErrors(form, error, (name) => errorIds[name] ?? `${id}-${name}-error`);
      toast(shown ? 'Revisa los campos marcados.' : error.message, { type: 'error' });
    }
  });
}

const section = (n, title, body) => `
  <section class="ai-section">
    <h4 class="ai-section__title"><span class="ai-section__n" aria-hidden="true">${n}</span>${title}</h4>
    ${body}
  </section>`;

const warningsHtml = (warnings) => (warnings.length ? `
  <div class="note note--warning ai-warnings">${icon('i-alert')}
    <div><strong>Advertencias del asistente</strong><ul>${warnings.map((w) => `<li>${escapeHtml(w)}</li>`).join('')}</ul></div>
  </div>` : '');

function contentHtml(query, status) {
  const response = query.aiResponse;
  if (status === 'PENDING') return searchingHtml();

  if (status === 'COMMUNICATION_ERROR' || status === 'FAILED') {
    const reason = failureText(query)
      ?? (status === 'FAILED' ? 'El servicio de IA no pudo procesar la consulta.' : 'No hubo comunicación con el servicio de IA (n8n).');
    return `<div class="note note--danger">${icon('i-alert')}<span><strong>${status === 'FAILED' ? 'El asistente no pudo completar el análisis.' : 'No se pudo contactar al asistente.'}</strong>
      ${escapeHtml(reason)}${query.failureCode ? ` <code>${escapeHtml(query.failureCode)}</code>` : ''}
      <br>La consulta quedó registrada; puedes intentarlo de nuevo.</span></div>`;
  }

  const parts = parseResponse(response);

  if (status === 'NO_RESULTS') {
    return `
      <div class="note note--warning">${icon('i-info')}<span><strong>Información insuficiente.</strong>
        No se encontraron documentos verificados suficientemente relevantes, así que el asistente no propone una solución.
        Conviene escalar el incidente a un técnico o documentar el caso en la base de conocimiento.</span></div>
      ${warningsHtml(parts.warnings)}`;
  }

  return `
    <div class="ai-metrics">
      <div><span class="detail-label">Confianza</span>${meter(response.confidenceScore, 'Confianza')}</div>
      <div><span class="detail-label">Fuentes</span><span>${response.sources.length}</span></div>
      <div><span class="detail-label">Tiempo de respuesta</span><span>${response.processingTimeMs != null ? `${(response.processingTimeMs / 1000).toLocaleString('es-CO', { maximumFractionDigits: 1 })} s` : '—'}</span></div>
    </div>
    ${parts.explanation ? section(1, 'Diagnóstico orientativo', `<p class="detail-text">${escapeHtml(parts.explanation)}</p>`) : ''}
    ${parts.solution ? section(2, 'Solución sugerida', `<p class="detail-text ai-solution">${escapeHtml(parts.solution)}</p>`) : ''}
    ${section(3, 'Pasos recomendados', parts.steps.length
      ? `<ol class="ai-steps">${parts.steps.map((s) => `<li>${escapeHtml(s)}</li>`).join('')}</ol>`
      : '<p class="cell-muted">La respuesta no incluye pasos.</p>')}
    ${section(4, `Fuentes documentales y similitud (${response.sources.length})`, sourcesHtml(response.sources))}
    ${warningsHtml(parts.warnings)}`;
}

/**
 * Pinta una consulta en `container`.
 * @param {object} query AiQueryHistoryResponseDTO
 * @param {{users?:Array<{id,name}>, onRated?:Function, heading?:string}} options
 */
export function renderAiQuery(container, query, { users = [], onRated, heading } = {}) {
  const status = estadoConsulta(query);
  const response = query.aiResponse;

  container.innerHTML = `
    <article class="card ai-card ai-card--${status.toLowerCase()}">
      <header class="ai-card__head">
        <div class="ai-card__identity">
          <span class="ai-avatar" aria-hidden="true">${icon('i-sparkles')}</span>
          <div>
            <h3 class="card__title">${escapeHtml(heading ?? 'Asistente IA')}</h3>
            <p class="cell-muted">Consulta #${query.aiQueryId} · ${formatDateTime(query.queriedAt)}</p>
          </div>
        </div>
        ${badge(ESTADO_CONSULTA, status)}
      </header>
      ${contentHtml(query, status)}
      <div data-rating-slot></div>
      ${query.sentPrompt ? `
        <details class="ai-prompt">
          <summary>Ver la consulta enviada</summary>
          <p>${escapeHtml(query.sentPrompt)}</p>
        </details>` : ''}
    </article>`;

  const slot = container.querySelector('[data-rating-slot]');
  if (response?.rating) slot.innerHTML = ratingSummaryHtml(response.rating);
  else if (response?.aiResponseId) mountRatingForm(slot, response.aiResponseId, users, onRated);
}

// Texto corto de la respuesta para listados.
export function responseSummary(query) {
  const status = estadoConsulta(query);
  if (status === 'PENDING') return 'Esperando respuesta…';
  if (status === 'COMMUNICATION_ERROR' || status === 'FAILED') return failureText(query) ?? 'Error al procesar la consulta';
  if (status === 'NO_RESULTS') return 'Sin información suficiente en la base de conocimiento';
  const parts = parseResponse(query.aiResponse);
  return parts.solution || parts.explanation || '—';
}
