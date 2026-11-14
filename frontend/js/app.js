// Punto de entrada: construye la navegación, controla la sidebar,
// vigila el estado de la API y arranca el router.

import { visibleRoutes } from './routes.js';
import { startRouter, navigate } from './router.js';
import { initSearch } from './search.js';
import { checkHealth } from './api.js';
import { getApiBaseUrl } from './config.js';
import { icon, escapeHtml, avatar, toast } from './ui.js';
import { currentUser, isAdmin, ROLE_LABEL } from './auth.js';
import { logout } from './services/auth.service.js';

const app = document.getElementById('app');
const SIDEBAR_KEY = 'incidentes-ia:sidebar';
const STATUS_INTERVAL_MS = 30000;

/* ---------- Navegación lateral (agrupada por sección, solo lo que el rol puede abrir) ---------- */
function renderNav() {
  const groups = new Map();
  visibleRoutes().filter((route) => route.nav !== false).forEach((route) => {
    const name = route.group ?? 'General';
    if (!groups.has(name)) groups.set(name, []);
    groups.get(name).push(route);
  });

  document.getElementById('nav-list').innerHTML = [...groups].map(([name, items]) => `
    <li class="nav-group">
      <span class="nav-group__title label" aria-hidden="true">${escapeHtml(name)}</span>
      <ul class="nav-group__list" role="list">
        ${items.map((route) => `
          <li>
            <a class="nav-link" href="#/${route.path}" data-route="${route.path}"
               data-label="${escapeHtml(route.title)}" aria-label="${escapeHtml(route.title)}">
              <span class="nav-link__icon">${icon(route.icon)}</span>
              <span class="label">${escapeHtml(route.navLabel ?? route.title)}</span>
              ${route.rf ? `<span class="nav-link__rf label" aria-hidden="true">${route.rf}</span>` : ''}
            </a>
          </li>`).join('')}
      </ul>
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

  // Sin preferencia guardada: contraída en pantallas medianas, y sigue el tamaño de la ventana
  // (abrir la página en una ventana pequeña y maximizarla expande el menú).
  const mediumScreen = window.matchMedia('(max-width: 1023px)');
  const saved = readSidebarPref();
  setCollapsed(saved ? saved === 'collapsed' : mediumScreen.matches);
  mediumScreen.addEventListener('change', (event) => {
    if (!readSidebarPref()) setCollapsed(event.matches);
  });

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

  // Tarjeta de estado en el pie de la sidebar (misma información, más detalle)
  const cardEl = document.getElementById('sidebar-status');
  const cardText = cardEl?.querySelector('[data-status-text]');
  const cardHost = cardEl?.querySelector('[data-status-host]');

  const setState = (state) => {
    statusEl.dataset.state = state;
    textEl.textContent = STATUS_TEXT[state];
    statusEl.title = `${STATUS_TEXT[state]} · ${getApiBaseUrl()}`;
    if (cardEl) {
      cardEl.dataset.state = state;
      cardText.textContent = STATUS_TEXT[state];
      cardHost.textContent = getApiBaseUrl().replace(/^https?:\/\//, '');
    }
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

/* ---------- Usuario de la sesión ---------- */
function renderUser() {
  const user = currentUser();
  const chip = document.getElementById('user-chip');
  chip.hidden = !user;
  if (user) {
    chip.dataset.role = user.role;
    chip.querySelector('[data-user-avatar]').innerHTML = avatar(user.fullName);
    chip.querySelector('[data-user-name]').textContent = user.fullName;
    chip.querySelector('[data-user-role]').textContent = ROLE_LABEL[user.role] ?? user.role;
    chip.title = `${user.fullName} · ${user.email}`;
  }
  // El estado de la API enlaza a Configuración solo para quien puede abrirla
  document.querySelectorAll('#api-status, #sidebar-status').forEach((link) => {
    if (isAdmin()) link.setAttribute('href', '#/configuracion');
    else link.removeAttribute('href');
  });
}

function initSession() {
  document.getElementById('logout').addEventListener('click', () => {
    logout();
    toast('Cerraste sesión.', { type: 'info' });
    navigate('login');
  });
  // Al iniciar o cerrar sesión cambian el menú y el usuario de la barra superior
  window.addEventListener('auth:changed', () => {
    renderNav();
    renderUser();
  });
  // El backend rechazó el token (vencido o inválido): de vuelta al login
  window.addEventListener('auth:expired', () => {
    toast('Tu sesión expiró. Inicia sesión de nuevo.', { type: 'error' });
    navigate('login');
  });
}

renderNav();
renderUser();
initSession();
initSidebar();
initSearch();
initStatusMonitor();
startRouter();
