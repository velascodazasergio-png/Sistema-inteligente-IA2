// Estados de error compartidos por los módulos.

import { icon, escapeHtml } from '../ui.js';
import { getApiBaseUrl } from '../config.js';
import { isAdmin } from '../auth.js';

// Bloque de error con botón "Reintentar". El texto viene de ApiError,
// que ya traduce timeouts, falta de conexión y errores de Spring.
export function errorState(container, error, onRetry) {
  const offline = error?.code === 'NETWORK_ERROR';
  container.innerHTML = `
    <div class="card empty-state" role="alert">
      <span class="empty-state__icon empty-state__icon--danger">${icon('i-alert')}</span>
      <h2 class="empty-state__title">${offline ? 'No hay conexión con el backend' : 'No se pudieron cargar los datos'}</h2>
      <p class="empty-state__text">${escapeHtml(error?.message ?? 'Error desconocido.')}</p>
      ${offline ? `<p class="field__hint">Comprueba que Spring Boot esté en ejecución en <code>${escapeHtml(getApiBaseUrl())}</code>.</p>` : ''}
      <div class="cluster">
        ${onRetry ? `<button class="btn btn--primary" type="button" data-retry>${icon('i-refresh')}Reintentar</button>` : ''}
        ${offline && isAdmin() ? `<a class="btn btn--ghost" href="#/configuracion">Configuración</a>` : ''}
      </div>
    </div>`;
  container.querySelector('[data-retry]')?.addEventListener('click', onRetry);
}

// Mensaje simple dentro de una tarjeta (sin datos, sin selección…).
export const emptyHtml = (title, text, iconName = 'i-inbox') => `
  <div class="empty-state empty-state--inline">
    <span class="empty-state__icon">${icon(iconName)}</span>
    <h3 class="empty-state__title">${escapeHtml(title)}</h3>
    ${text ? `<p class="empty-state__text">${escapeHtml(text)}</p>` : ''}
  </div>`;
