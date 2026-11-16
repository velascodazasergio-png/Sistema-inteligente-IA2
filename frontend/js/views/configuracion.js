// Configuration view (administrator): where the data is stored and connection check.
// Nothing is configured or saved in the browser: the API address comes from where the page is served.

import { pageHeader, icon, setBusy, toast } from '../ui.js';
import { getApiBaseUrl } from '../config.js';
import { checkHealth } from '../api.js';

// Configuration view: API address, connection test and storage summary
export function render(container, { signal }) {
  container.innerHTML = `
    ${pageHeader({
      title: 'Configuración',
      icon: 'i-settings',
      description: 'Conexión con el backend. Todos los datos, las sesiones y las preferencias se guardan en la base de datos.',
    })}

    <div class="grid-2">
      <section class="card" aria-labelledby="api-config-title">
        <h2 class="card__title" id="api-config-title">Conexión con el backend</h2>
        <p class="card__subtitle">
          El frontend solo se comunica con la API REST de Spring Boot.
          Nunca llama directamente a n8n, Qdrant ni OpenRouter.
        </p>
        <dl class="kv">
          <dt>URL de la API</dt>
          <dd><code data-field="url"></code></dd>
          <dt>Estado</dt>
          <dd data-field="state"><span class="badge">Sin comprobar</span></dd>
          <dt>Última comprobación</dt>
          <dd data-field="checked">—</dd>
        </dl>
        <div class="form__actions">
          <button class="btn btn--primary" type="button" data-action="test">${icon('i-check')}Probar conexión</button>
        </div>
        <p class="field__hint">
          La URL se deduce del lugar donde se sirve la página: por HTTPS, el mismo origen (nginx reenvía <code>/api</code>);
          en desarrollo, <code>http://localhost:8080</code>.
        </p>
      </section>

      <section class="card" aria-labelledby="storage-title">
        <h2 class="card__title" id="storage-title">Dónde se guarda la información</h2>
        <p class="card__subtitle">Todo queda en la base de datos del backend (PostgreSQL).</p>
        <dl class="kv">
          <dt>Datos del sistema</dt>
          <dd>Incidentes, artículos, consultas a la IA y calificaciones</dd>
          <dt>Sesiones</dt>
          <dd><code>user_sessions</code></dd>
          <dt>Preferencias</dt>
          <dd><code>user_preferences</code></dd>
          <dt>En el navegador</dt>
          <dd>Solo el token que identifica tu sesión</dd>
        </dl>
      </section>
    </div>`;

  const testButton = container.querySelector('[data-action="test"]');
  // Element of the connection result by name
  const field = (name) => container.querySelector(`[data-field="${name}"]`);

  // Checks whether the backend answers and shows the result
  const runCheck = async ({ silent = false } = {}) => {
    const url = getApiBaseUrl();
    setBusy(testButton, true, 'Probando…');
    const ok = await checkHealth(url);
    setBusy(testButton, false);
    if (signal.aborted) return;

    field('url').textContent = url;
    field('state').innerHTML = ok
      ? '<span class="badge badge--success">Responde</span>'
      : '<span class="badge badge--danger">Sin respuesta</span>';
    field('checked').textContent = new Date().toLocaleTimeString('es-CO');
    if (!silent) {
      toast(ok ? `El backend responde en ${url}.` : `No hay respuesta en ${url}. ¿Está Spring Boot en ejecución?`,
        { type: ok ? 'success' : 'error' });
    }
  };

  testButton.addEventListener('click', () => runCheck());
  runCheck({ silent: true });
}
