// Tarjeta de una consulta a la IA (AiQueryHistoryResponseDTO): estado,
// diagnóstico, pasos, fuentes con similitud y calificación (RF05 / RF08).
// La usan el Asistente y el Historial.

import { icon, escapeHtml, toast, setBusy } from '../ui.js';
import { createStarRating, starsHtml } from './rating.js';
import { ESTADO_CONSULTA, badge, estadoConsulta } from '../catalogs.js';
import { formatDateTime, formatRatio } from '../format.js';
import { rateResponse } from '../services/ai-assistance.service.js';

// Solo enlaces http(s): una URL "javascript:" de una fuente no debe ejecutarse.
const safeUrl = (value) => {
  try {
    const url = new URL(value);
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null;
  } catch {
    return null;
  }
};

// "1. Hacer A. 2. Hacer B." o texto con saltos de línea → lista de pasos.
function stepsHtml(text) {
  if (!text?.trim()) return '<p class="cell-muted">La respuesta no incluye pasos.</p>';
  const steps = text.split(/\n+|\s+(?=\d+[.)]\s)/)
    .map((s) => s.replace(/^\s*\d+[.)]\s*/, '').trim())
    .filter(Boolean);
  return steps.length > 1
    ? `<ol class="ai-steps">${steps.map((s) => `<li>${escapeHtml(s)}</li>`).join('')}</ol>`
    : `<p class="detail-text">${escapeHtml(text)}</p>`;
}

const meter = (value, label) => `
  <span class="meter" role="img" aria-label="${escapeHtml(label)}: ${formatRatio(value)}">
    <span class="meter__bar"><span style="width: ${Math.max(0, Math.min(1, value ?? 0)) * 100}%"></span></span>
    <span class="meter__value">${formatRatio(value)}</span>
  </span>`;

function sourcesHtml(sources) {
  return `
    <ul class="ai-sources">
      ${sources.map((s) => {
        const url = safeUrl(s.referenceUrl);
        return `
          <li class="ai-source">
            <div class="ai-source__head">
              <span class="ai-source__title">${icon('i-book')}${escapeHtml(s.sourceTitle ?? 'Fuente sin título')}</span>
              ${meter(s.similarityScore, 'Similitud')}
            </div>
            ${s.citedSnippet ? `<blockquote class="ai-source__snippet">${escapeHtml(s.citedSnippet)}</blockquote>` : ''}
            ${url ? `<a class="ai-source__link" href="${escapeHtml(url)}" target="_blank" rel="noopener noreferrer">${escapeHtml(url)}${icon('i-external')}</a>` : ''}
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
          placeholder="¿Qué funcionó o qué faltó?"></textarea>
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
        feedbackComment: data.feedbackComment,
      });
      slot.innerHTML = ratingSummaryHtml(rating);
      toast('Gracias: la calificación quedó registrada.', { type: 'success' });
      onRated?.(rating);
    } catch (error) {
      setBusy(button, false);
      stars.setDisabled(false);
      toast(error.message, { type: 'error' });
    }
  });
}

/**
 * Pinta una consulta en `container`.
 * @param {object} query AiQueryHistoryResponseDTO
 * @param {{users?:Array<{id,name}>, onRated?:Function, heading?:string}} options
 */
export function renderAiQuery(container, query, { users = [], onRated, heading } = {}) {
  const status = estadoConsulta(query);
  const response = query.aiResponse;

  let content;
  if (status === 'PENDING') {
    content = `<p class="ai-wait"><span class="spinner" aria-hidden="true"></span>El asistente está analizando el incidente…</p>`;
  } else if (status === 'COMMUNICATION_ERROR') {
    content = `<p class="note note--danger">${icon('i-alert')}<span>No hubo comunicación con el servicio de IA (n8n). La consulta quedó registrada con estado de error; puedes intentarlo de nuevo.</span></p>`;
  } else if (status === 'NO_RESULTS') {
    content = `
      <p class="note note--warning">${icon('i-info')}<span><strong>Información insuficiente.</strong> No se encontraron documentos relevantes en la base de conocimiento, así que el asistente no puede respaldar una solución. Conviene escalar el incidente a un técnico.</span></p>
      ${response?.suggestedDiagnosis ? `<h4 class="detail-label">Observación del asistente</h4><p class="detail-text">${escapeHtml(response.suggestedDiagnosis)}</p>` : ''}`;
  } else {
    content = `
      <div class="ai-metrics">
        <div><span class="detail-label">Confianza</span>${meter(response.confidenceScore, 'Confianza')}</div>
        <div><span class="detail-label">Tiempo de respuesta</span><span>${response.processingTimeMs != null ? `${(response.processingTimeMs / 1000).toLocaleString('es-CO', { maximumFractionDigits: 1 })} s` : '—'}</span></div>
      </div>
      <h4 class="detail-label">Diagnóstico sugerido</h4>
      <p class="detail-text">${escapeHtml(response.suggestedDiagnosis)}</p>
      <h4 class="detail-label">Pasos recomendados</h4>
      ${stepsHtml(response.solutionSteps)}
      <h4 class="detail-label">Fuentes consultadas (${response.sources.length})</h4>
      ${sourcesHtml(response.sources)}`;
  }

  container.innerHTML = `
    <article class="card ai-card ai-card--${status.toLowerCase()}">
      <header class="ai-card__head">
        <div>
          ${heading ? `<h3 class="card__title">${escapeHtml(heading)}</h3>` : ''}
          <p class="cell-muted">Consulta #${query.aiQueryId} · ${formatDateTime(query.queriedAt)}</p>
        </div>
        ${badge(ESTADO_CONSULTA, status)}
      </header>
      ${content}
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
