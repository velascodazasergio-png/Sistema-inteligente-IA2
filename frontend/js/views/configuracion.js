// Vista de configuración: URL base de la API y comprobación de conexión.

import { pageHeader, icon, toast, escapeHtml, setBusy } from '../ui.js';
import {
  getApiBaseUrl,
  setApiBaseUrl,
  resetApiBaseUrl,
  normalizeBaseUrl,
  DEFAULT_API_BASE_URL,
} from '../config.js';
import { checkHealth } from '../api.js';

function validateUrl(value) {
  if (!value) return 'Escribe la URL base de la API.';
  try {
    const url = new URL(value);
    if (!['http:', 'https:'].includes(url.protocol)) return 'La URL debe empezar por http:// o https://.';
  } catch {
    return 'La URL no es válida. Ejemplo: http://localhost:8080';
  }
  return '';
}

export function render(container, { signal }) {
  container.innerHTML = `
    ${pageHeader({
      title: 'Configuración',
      icon: 'i-settings',
      description: 'Ajustes guardados solo en este navegador. No se envían al servidor.',
    })}

    <div class="grid-2">
      <section class="card" aria-labelledby="api-config-title">
        <h2 class="card__title" id="api-config-title">Conexión con el backend</h2>
        <p class="card__subtitle">
          El frontend solo se comunica con la API REST de Spring Boot.
          Nunca llama directamente a n8n, Qdrant ni OpenRouter.
        </p>

        <form class="form" id="api-form" novalidate>
          <div class="field">
            <label for="api-url">URL base de la API</label>
            <input class="input" id="api-url" name="apiUrl" type="url" inputmode="url" required
                   spellcheck="false" autocomplete="url" aria-describedby="api-url-hint api-url-error"
                   value="${escapeHtml(getApiBaseUrl())}">
            <p class="field__hint" id="api-url-hint">Por defecto: <code>${DEFAULT_API_BASE_URL}</code></p>
            <p class="field__error" id="api-url-error" aria-live="polite"></p>
          </div>

          <div class="form__actions">
            <button class="btn btn--primary" type="submit">${icon('i-check')}Guardar</button>
            <button class="btn btn--ghost" type="button" data-action="test">Probar conexión</button>
            <button class="btn btn--ghost" type="button" data-action="reset">Restablecer</button>
          </div>
        </form>
      </section>

      <section class="card" aria-labelledby="api-info-title">
        <h2 class="card__title" id="api-info-title">Estado de la conexión</h2>
        <p class="card__subtitle">Resultado de la última comprobación.</p>
        <dl class="kv">
          <dt>URL activa</dt>
          <dd><code data-field="url"></code></dd>
          <dt>Estado</dt>
          <dd data-field="state"><span class="badge">Sin comprobar</span></dd>
          <dt>Última comprobación</dt>
          <dd data-field="checked">—</dd>
        </dl>
        <p class="field__hint">
          La comprobación solo verifica que el servidor responda (consulta <code>/api-docs</code>).
          Para leer datos, el backend también debe permitir CORS desde el origen de este frontend.
        </p>
      </section>
    </div>`;

  const form = container.querySelector('#api-form');
  const input = container.querySelector('#api-url');
  const errorEl = container.querySelector('#api-url-error');
  const testButton = container.querySelector('[data-action="test"]');
  const field = (name) => container.querySelector(`[data-field="${name}"]`);
  let touched = false;

  const validate = () => {
    const message = validateUrl(input.value.trim());
    errorEl.textContent = message;
    input.setAttribute('aria-invalid', String(Boolean(message)));
    return !message;
  };

  const notifyChange = () => window.dispatchEvent(new CustomEvent('api:config-changed'));

  const runCheck = async (url = getApiBaseUrl(), { silent = false } = {}) => {
    setBusy(testButton, true, 'Probando…');
    const ok = await checkHealth(url);
    setBusy(testButton, false);
    if (signal.aborted) return;

    field('url').textContent = normalizeBaseUrl(url);
    field('state').innerHTML = ok
      ? '<span class="badge badge--success">Responde</span>'
      : '<span class="badge badge--danger">Sin respuesta</span>';
    field('checked').textContent = new Date().toLocaleTimeString('es-CO');

    if (!silent) {
      toast(
        ok ? `El backend responde en ${normalizeBaseUrl(url)}.` : `No hay respuesta en ${normalizeBaseUrl(url)}. ¿Está Spring Boot en ejecución?`,
        { type: ok ? 'success' : 'error' },
      );
    }
  };

  // Validación en tiempo real (después del primer contacto con el campo).
  input.addEventListener('input', () => { if (touched) validate(); });
  input.addEventListener('blur', () => { touched = true; validate(); });

  form.addEventListener('submit', (event) => {
    event.preventDefault();
    touched = true;
    if (!validate()) {
      input.focus();
      return;
    }
    input.value = setApiBaseUrl(input.value);
    toast('URL de la API guardada.', { type: 'success' });
    notifyChange();
    runCheck(input.value, { silent: true });
  });

  testButton.addEventListener('click', () => {
    touched = true;
    if (validate()) runCheck(input.value.trim());
  });

  container.querySelector('[data-action="reset"]').addEventListener('click', () => {
    resetApiBaseUrl();
    input.value = DEFAULT_API_BASE_URL;
    validate();
    toast('Se restableció la URL por defecto.', { type: 'info' });
    notifyChange();
    runCheck(DEFAULT_API_BASE_URL, { silent: true });
  });

  runCheck(getApiBaseUrl(), { silent: true });
}
