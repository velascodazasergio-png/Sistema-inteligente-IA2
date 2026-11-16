// Entry point: builds the navigation, controls the sidebar,
// watches the API status and starts the router.

import { visibleRoutes } from './routes.js';
import { startRouter, navigate } from './router.js';
import { initSearch } from './search.js';
import { checkHealth } from './api.js';
import { getApiBaseUrl } from './config.js';
import { icon, escapeHtml, avatar, toast } from './ui.js';
import { currentUser, isAdmin, ROLE_LABEL } from './auth.js';
import { logout, restoreSession } from './services/auth.service.js';
import { getPreference, savePreference, loadPreferences, clearPreferences } from './services/preferences.service.js';

const app = document.getElementById('app');
const STATUS_INTERVAL_MS = 30000;

/* ---------- Side navigation (grouped by section, only what the role can open) ---------- */
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

// Sidebar behaviour: collapse button, mobile drawer and responsive default
function initSidebar() {
  const toggle = document.getElementById('sidebar-toggle');
  const toggleLabel = toggle.querySelector('.label');
  const menuButton = document.getElementById('menu-open');
  const sidebar = document.getElementById('sidebar');

  // Collapses or expands the sidebar and updates the toggle button
  const setCollapsed = (collapsed) => {
    app.dataset.sidebar = collapsed ? 'collapsed' : 'expanded';
    const text = collapsed ? 'Expandir menú' : 'Contraer menú';
    toggle.setAttribute('aria-expanded', String(!collapsed));
    toggle.setAttribute('aria-label', text);
    toggle.dataset.label = text;
    toggleLabel.textContent = text;
  };

  // The preference is stored in the database for each user. Without one: collapsed on medium screens,
  // following the window size (opening the page in a small window and maximizing it expands the menu).
  const mediumScreen = window.matchMedia('(max-width: 1023px)');
  // Applies the stored preference, or the automatic behaviour when there is none
  const applyPreference = () => {
    const saved = getPreference('sidebarCollapsed');
    setCollapsed(typeof saved === 'boolean' ? saved : mediumScreen.matches);
  };
  applyPreference();
  window.addEventListener('preferences:loaded', applyPreference);
  mediumScreen.addEventListener('change', (event) => {
    if (typeof getPreference('sidebarCollapsed') !== 'boolean') setCollapsed(event.matches);
  });

  toggle.addEventListener('click', () => {
    const collapsed = app.dataset.sidebar !== 'collapsed';
    setCollapsed(collapsed);
    if (currentUser()) savePreference('sidebarCollapsed', collapsed);
  });

  // Mobile drawer
  const openNav = () => {
    app.setAttribute('data-nav-open', '');
    menuButton.setAttribute('aria-expanded', 'true');
    sidebar.querySelector('.nav-link')?.focus();
  };

  // Closes the mobile drawer and returns the focus to the menu button
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

/* ---------- API status ---------- */
const STATUS_TEXT = {
  checking: 'Comprobando API…',
  online: 'API conectada',
  offline: 'API sin conexión',
};

// Shows whether the API is online and checks it periodically
function initStatusMonitor() {
  const statusEl = document.getElementById('api-status');
  const textEl = statusEl.querySelector('.status__text');

  // Status card at the bottom of the sidebar (same information, more detail)
  const cardEl = document.getElementById('sidebar-status');
  const cardText = cardEl?.querySelector('[data-status-text]');
  const cardHost = cardEl?.querySelector('[data-status-host]');

  // Paints the API status (checking, online, offline)
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

  // Checks the API now and updates the status
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
}

/* ---------- Logged-in user ---------- */
function renderUser() {
  const user = currentUser();
  const chip = document.getElementById('user-chip');
  chip.hidden = !user;
  if (user) {
    chip.dataset.role = user.role;
    chip.querySelector('[data-user-avatar]').innerHTML = avatar(user.fullName);
    chip.querySelector('[data-user-name]').textContent = user.fullName;
    chip.querySelector('[data-user-role]').textContent = ROLE_LABEL[user.role] ?? user.role;
    chip.title = [user.fullName, user.email, user.area].filter(Boolean).join(' · ');
  }
  // The API status links to Configuration only for users who can open it
  document.querySelectorAll('#api-status, #sidebar-status').forEach((link) => {
    if (isAdmin()) link.setAttribute('href', '#/configuracion');
    else link.removeAttribute('href');
  });
}

// Logout button, user chip in the top bar and reaction to session changes
function initSession() {
  document.getElementById('logout').addEventListener('click', async () => {
    await logout();
    toast('Cerraste sesión.', { type: 'info' });
    navigate('login');
  });
  // Logging in or out changes the menu, the user shown in the top bar and the preferences
  window.addEventListener('auth:changed', (event) => {
    renderNav();
    renderUser();
    if (event.detail?.user) loadPreferences();
    else {
      clearPreferences();
      window.dispatchEvent(new CustomEvent('preferences:loaded'));
    }
  });
  // Another tab logged in: read the user of the new session from the database
  window.addEventListener('auth:restore', async () => {
    await restoreSession();
    if (!currentUser()) navigate('login');
  });
  // The backend rejected the token (expired or invalid): back to the login screen
  window.addEventListener('auth:expired', () => {
    toast('Tu sesión expiró. Inicia sesión de nuevo.', { type: 'error' });
    navigate('login');
  });
}

// Startup: the session and the preferences are read from the database before showing any section
initSession();
initSidebar();
initSearch();
initStatusMonitor();
if (await restoreSession()) await loadPreferences();
renderNav();
renderUser();
startRouter();
