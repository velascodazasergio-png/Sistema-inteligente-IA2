// Punto de entrada: construye la navegación, controla la sidebar,
// vigila el estado de la API y arranca el router.

import { routes } from './routes.js';
import { startRouter } from './router.js';
import { initSearch } from './search.js';
import { checkHealth } from './api.js';
import { getApiBaseUrl } from './config.js';
import { icon, escapeHtml } from './ui.js';

const app = document.getElementById('app');
const SIDEBAR_KEY = 'incidentes-ia:sidebar';
const STATUS_INTERVAL_MS = 30000;

/* ---------- Navegación lateral ---------- */
function renderNav() {
  document.getElementById('nav-list').innerHTML = routes.map((route) => `
    <li>
      <a class="nav-link" href="#/${route.path}" data-route="${route.path}"
         data-label="${escapeHtml(route.title)}" aria-label="${escapeHtml(route.title)}">
        ${icon(route.icon)}
        <span class="label">${escapeHtml(route.title)}</span>
        ${route.rf ? `<span class="nav-link__rf label" aria-hidden="true">${route.rf}</span>` : ''}
      </a>
    </li>`).join('');
}

function readSidebarPref() {
  try {
    return localStorage.getItem(SIDEBAR_KEY);
  } catch {
    return null;
  }
}

function saveSidebarPref(value) {
  try {
    localStorage.setItem(SIDEBAR_KEY, value);
  } catch {
    // Sin almacenamiento: la preferencia dura solo esta sesión.
  }
}

function initSidebar() {
  const toggle = document.getElementById('sidebar-toggle');
  const toggleLabel = toggle.querySelector('.label');
  const menuButton = document.getElementById('menu-open');
  const sidebar = document.getElementById('sidebar');

  const setCollapsed = (collapsed) => {
    app.dataset.sidebar = collapsed ? 'collapsed' : 'expanded';
    const text = collapsed ? 'Expandir menú' : 'Contraer menú';
    toggle.setAttribute('aria-expanded', String(!collapsed));
    toggle.setAttribute('aria-label', text);
    toggle.dataset.label = text;
    toggleLabel.textContent = text;
  };

  // Sin preferencia guardada: contraída en pantallas medianas.
  const saved = readSidebarPref();
  setCollapsed(saved ? saved === 'collapsed' : window.matchMedia('(max-width: 1023px)').matches);

  toggle.addEventListener('click', () => {
    const collapsed = app.dataset.sidebar !== 'collapsed';
    setCollapsed(collapsed);
    saveSidebarPref(collapsed ? 'collapsed' : 'expanded');
  });

  // Panel móvil
  const openNav = () => {
    app.setAttribute('data-nav-open', '');
    menuButton.setAttribute('aria-expanded', 'true');
    sidebar.querySelector('.nav-link')?.focus();
  };

  const closeNav = () => {
    if (!app.hasAttribute('data-nav-open')) return;
    const focusWasInside = sidebar.contains(document.activeElement);
    app.removeAttribute('data-nav-open');
    menuButton.setAttribute('aria-expanded', 'false');
    if (focusWasInside) menuButton.focus();
  };

  menuButton.addEventListener('click', openNav);
  document.getElementById('sidebar-close').addEventListener('click', closeNav);
  document.getElementById('sidebar-backdrop').addEventListener('click', closeNav);
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') closeNav();
  });
  window.addEventListener('route:change', closeNav);
  window.matchMedia('(max-width: 767px)').addEventListener('change', closeNav);
}

/* ---------- Estado de la API ---------- */
const STATUS_TEXT = {
  checking: 'Comprobando API…',
  online: 'API conectada',
  offline: 'API sin conexión',
};

function initStatusMonitor() {
  const statusEl = document.getElementById('api-status');
  const textEl = statusEl.querySelector('.status__text');

  const setState = (state) => {
    statusEl.dataset.state = state;
    textEl.textContent = STATUS_TEXT[state];
    statusEl.title = `${STATUS_TEXT[state]} · ${getApiBaseUrl()}`;
  };

  const refresh = async ({ showChecking = false } = {}) => {
    if (showChecking) setState('checking');
    setState((await checkHealth()) ? 'online' : 'offline');
  };

  refresh({ showChecking: true });
  setInterval(() => {
    if (document.visibilityState === 'visible') refresh();
  }, STATUS_INTERVAL_MS);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') refresh();
  });
  window.addEventListener('api:config-changed', () => refresh({ showChecking: true }));
}

renderNav();
initSidebar();
initSearch();
initStatusMonitor();
startRouter();
