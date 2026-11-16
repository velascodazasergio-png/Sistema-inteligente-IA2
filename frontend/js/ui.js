// Shared UI utilities: icons, HTML escaping,
// page headers and toast notifications.

// SVG icon from the sprite in index.html
export const icon = (name, className = '') =>
  `<svg class="icon ${className}" aria-hidden="true" focusable="false"><use href="#${name}"></use></svg>`;

const HTML_ESCAPES = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };

// Every text coming from the backend or the user must go through here
// before being inserted with innerHTML.
export function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (char) => HTML_ESCAPES[char]);
}

// Lowercase and without accents: for accent-insensitive searches.
export const normalizeText = (text) =>
  String(text ?? '').normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim();

// Page header. Accepts the full route ({ rf, title, description, icon })
// or the loose fields ({ eyebrow, title, description, icon }).
export function pageHeader({ eyebrow, rf, title, description, icon: iconName }) {
  const tag = eyebrow ?? rf;
  return `
    <header class="page-header">
      <span class="page-header__icon" aria-hidden="true">${icon(iconName ?? 'i-logo')}</span>
      <div class="page-header__text">
        <div class="page-header__meta">
          ${tag ? `<span class="page-header__eyebrow">${escapeHtml(tag)}</span>` : ''}
          <span class="page-header__crumb">Incidentes IA / ${escapeHtml(title)}</span>
        </div>
        <h1 class="page-title" tabindex="-1">${escapeHtml(title)}</h1>
        ${description ? `<p class="page-header__desc">${escapeHtml(description)}</p>` : ''}
      </div>
    </header>`;
}

// Avatar with initials and a stable color derived from the name.
export function avatar(name, size = 'sm') {
  const clean = String(name ?? '').trim();
  const initials = clean.split(/\s+/).slice(0, 2).map((w) => w[0]?.toUpperCase() ?? '').join('') || '?';
  let hash = 0;
  for (const ch of clean) hash = (hash * 31 + ch.charCodeAt(0)) % 360;
  return `<span class="avatar avatar--${size}" style="--avatar-hue: ${hash}" aria-hidden="true">${escapeHtml(initials)}</span>`;
}

// Marks a button as busy while an async operation runs.
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

// Shows a temporary notification (info, success, warning, error)
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
  // Hides the notification with its exit animation
  const dismiss = () => {
    clearTimeout(timer);
    el.classList.add('toast--leaving');
    // Fallback in case animationend does not fire.
    setTimeout(() => el.remove(), 400);
    el.addEventListener('animationend', () => el.remove(), { once: true });
  };

  el.querySelector('.toast__close').addEventListener('click', dismiss);
  el.addEventListener('mouseenter', () => clearTimeout(timer));
  el.addEventListener('mouseleave', () => { timer = setTimeout(dismiss, duration); });

  region.append(el);
  timer = setTimeout(dismiss, duration);
}
