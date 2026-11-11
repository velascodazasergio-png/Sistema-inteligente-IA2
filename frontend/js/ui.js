// Utilidades de interfaz compartidas: iconos, escape de HTML,
// encabezados de página y notificaciones toast.

export const icon = (name, className = '') =>
  `<svg class="icon ${className}" aria-hidden="true" focusable="false"><use href="#${name}"></use></svg>`;

const HTML_ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };

// Todo texto que venga del backend o del usuario debe pasar por aquí
// antes de insertarse con innerHTML.
export function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => HTML_ESCAPES[char]);
}

export function pageHeader({ eyebrow, title, description }) {
  return `
    <header class="page-header">
      ${eyebrow ? `<span class="page-header__eyebrow">${escapeHtml(eyebrow)}</span>` : ''}
      <h1 class="page-title" tabindex="-1">${escapeHtml(title)}</h1>
      ${description ? `<p class="page-header__desc">${escapeHtml(description)}</p>` : ''}
    </header>`;
}

// Marca un botón como ocupado mientras dura una operación asíncrona.
export function setBusy(button, busy, busyLabel = 'Procesando…') {
  if (busy) {
    button.dataset.originalHtml = button.innerHTML;
    button.setAttribute('aria-busy', 'true');
    button.innerHTML = `<span class="spinner" aria-hidden="true"></span>${escapeHtml(busyLabel)}`;
  } else {
    button.removeAttribute('aria-busy');
    if (button.dataset.originalHtml) button.innerHTML = button.dataset.originalHtml;
    delete button.dataset.originalHtml;
  }
}

const TOAST_ICONS = { info: 'i-info', success: 'i-check', warning: 'i-alert', error: 'i-alert' };

export function toast(message, { type = 'info', duration = 4500 } = {}) {
  const region = document.getElementById('toast-region');
  if (!region) return;

  const el = document.createElement('div');
  el.className = `toast toast--${type}`;
  if (type === 'error') el.setAttribute('role', 'alert');
  el.innerHTML = `
    ${icon(TOAST_ICONS[type] ?? 'i-info')}
    <p class="toast__msg">${escapeHtml(message)}</p>
    <button class="toast__close" type="button" aria-label="Cerrar notificación">${icon('i-close')}</button>`;

  let timer;
  const dismiss = () => {
    clearTimeout(timer);
    el.classList.add('toast--leaving');
    // Respaldo por si animationend no se dispara.
    setTimeout(() => el.remove(), 400);
    el.addEventListener('animationend', () => el.remove(), { once: true });
  };

  el.querySelector('.toast__close').addEventListener('click', dismiss);
  el.addEventListener('mouseenter', () => clearTimeout(timer));
  el.addEventListener('mouseleave', () => { timer = setTimeout(dismiss, duration); });

  region.append(el);
  timer = setTimeout(dismiss, duration);
}
