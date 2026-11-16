// Hash-based router (#/section): navigation without reloading the page
// that works with any static server.

import { routes, resolveRoute } from './routes.js';
import { pageHeader, toast } from './ui.js';
import { getSession, currentUser, canAccess, homeFor } from './auth.js';

const view = document.getElementById('main');
const titleEl = document.getElementById('topbar-title');
const eyebrowEl = document.getElementById('topbar-eyebrow');

let controller = null;
let isFirstRender = true;

// Goes to a section (re-renders if it is already open)
export function navigate(path) {
  const hash = `#/${path}`;
  if (location.hash === hash) render();
  else location.hash = hash;
}

// Parameters of the current route: #/asistente?incidente=12 → URLSearchParams
export function routeParams() {
  return new URLSearchParams(location.hash.split('?')[1] ?? '');
}

// No section in the URL: the role's home (or the login screen without a session)
function currentPath() {
  return location.hash.replace(/^#\/?/, '').split(/[?/]/)[0] || homeFor(currentUser()?.role);
}

// Replaces the URL without adding to the history and renders it
const redirect = (path) => {
  history.replaceState(null, '', `#/${path}`);
  return render();
};

// Decides which section to show based on the session and the role.
// Returns the route to render or, when redirecting, the promise of the new render.
function guard(route) {
  const session = getSession();
  if (!route) return redirect(homeFor(session?.user?.role));
  if (!route.public && !session) {
    // No session: go to the login screen, remembering where the user wanted to go
    const next = location.hash.replace(/^#\/?/, '');
    history.replaceState(null, '', `#/login${next && !next.startsWith('login') ? `?next=${encodeURIComponent(next)}` : ''}`);
    return render();
  }
  if (route.path === 'login' && session) return redirect(homeFor(session.user.role));
  if (!canAccess(route)) {
    toast('Tu rol no tiene acceso a esa sección.', { type: 'error' });
    return redirect(homeFor(session.user.role));
  }
  return resolveRoute(route);
}

// Marks the active menu item and updates the title and accent color
function updateChrome(route) {
  document.querySelectorAll('.nav-link[data-route]').forEach((link) => {
    if (link.dataset.route === route.path) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  });
  // Each module has its own accent color (css/color.css)
  document.documentElement.dataset.module = route.path;
  titleEl.textContent = route.title;
  eyebrowEl.textContent = route.rf ?? 'Sistema';
  document.title = `${route.title} · Incidentes IA`;
}

// Renders the current section (cancels the previous one, loads the view module)
async function render() {
  const route = guard(routes.find((r) => r.path === currentPath()));
  if (route instanceof Promise) return route;
  // The login screen takes the whole window (no side or top bar)
  document.getElementById('app').dataset.layout = route.layout ?? 'app';

  // Cancels the requests of the previous view.
  controller?.abort();
  controller = new AbortController();
  const { signal } = controller;

  updateChrome(route);
  // An open dialog (detail, form) belongs to the previous section: it is closed when navigating.
  // This runs before rendering the new section, which may open its own (e.g. #/incidentes?id=12).
  document.querySelectorAll('dialog[open]').forEach((dialog) => dialog.close());

  try {
    const module = route.load ? await route.load() : await import('./views/pending.js');
    if (signal.aborted) return;

    view.replaceChildren();
    view.classList.remove('view--enter');
    void view.offsetWidth; // restarts the entry animation
    view.classList.add('view--enter');

    await module.render(view, { route, signal });
  } catch (error) {
    if (signal.aborted) return;
    console.error(error);
    view.innerHTML = pageHeader({ ...route, description: 'No se pudo cargar esta sección.' });
    toast('No se pudo cargar la sección. Recarga la página.', { type: 'error' });
  }

  // After navigating, focus moves to the title for screen readers and keyboard users.
  if (!isFirstRender) {
    window.scrollTo({ top: 0 });
    view.querySelector('.page-title')?.focus({ preventScroll: true });
  }
  isFirstRender = false;

  window.dispatchEvent(new CustomEvent('route:change', { detail: { route } }));
}

// Starts listening to URL changes and renders the first section
export function startRouter() {
  window.addEventListener('hashchange', render);
  render();
}
