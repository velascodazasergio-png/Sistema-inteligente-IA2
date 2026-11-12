// Vista para secciones sin endpoints en el backend todavía.
// No muestra datos de ejemplo: indica qué datos necesita la sección,
// para coordinar los contratos con la persona responsable del backend.

import { icon, escapeHtml, pageHeader } from '../ui.js';
import { getApiBaseUrl } from '../config.js';

const list = (items, className, iconName) => `
  <ul class="requirements ${className}">
    ${items.map((item) => `<li>${icon(iconName)}<span>${escapeHtml(item)}</span></li>`).join('')}
  </ul>`;

export function render(container, { route }) {
  container.innerHTML = `
    ${pageHeader(route)}

    <section class="card empty-state" aria-labelledby="pending-title">
      <span class="empty-state__icon">${icon('i-plug')}</span>
      <h2 class="empty-state__title" id="pending-title">Módulo pendiente de integración</h2>
      <p class="empty-state__text">
        Esta sección se construirá cuando el backend exponga los datos que necesita.
        Para no mostrar cifras inventadas, aquí no se usan datos de ejemplo.
      </p>

      ${route.available?.length ? `
        <div class="requirements-group">
          <h3>Disponible hoy en el backend</h3>
          ${list(route.available, 'requirements--ok', 'i-check')}
        </div>` : ''}

      ${route.needs?.length ? `
        <div class="requirements-group">
          <h3>Datos que necesita del backend</h3>
          ${list(route.needs, '', 'i-alert')}
        </div>` : ''}

      ${route.note ? `<p class="note">${icon('i-info')}<span>${escapeHtml(route.note)}</span></p>` : ''}

      <a class="btn btn--ghost" href="${escapeHtml(getApiBaseUrl())}/swagger-ui.html" target="_blank" rel="noopener">
        Ver contratos en Swagger ${icon('i-arrow-right')}
      </a>
    </section>`;
}
