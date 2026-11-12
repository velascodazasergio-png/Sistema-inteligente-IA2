// Router basado en hash (#/seccion): navegación sin recargar la página
// y compatible con cualquier servidor estático.

import { routes, DEFAULT_ROUTE } from './routes.js';
import { pageHeader, toast } from './ui.js';

const view = document.getElementById('main');
const titleEl = document.getElementById('topbar-title');
const eyebrowEl = document.getElementById('topbar-eyebrow');

let controller = null;
let isFirstRender = true;

export function navigate(path) {
  const hash = `#/${path}`;
  if (location.hash === hash) render();
  else location.hash = hash;
}

// Parámetros de la ruta actual: #/asistente?incidente=12 → URLSearchParams
export function routeParams() {
  return new URLSearchParams(location.hash.split('?')[1] ?? '');
}

function currentPath() {
  return location.hash.replace(/^#\/?/, '').split(/[?/]/)[0] || DEFAULT_ROUTE;
}

function updateChrome(route) {
  document.querySelectorAll('.nav-link[data-route]').forEach((link) => {
    if (link.dataset.route === route.path) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  });
  titleEl.textContent = route.title;
  eyebrowEl.textContent = route.rf ?? 'Sistema';
  document.title = `${route.title} · Incidentes IA`;
}

async function render() {
  const route = routes.find((r) => r.path === currentPath());
  if (!route) {
    history.replaceState(null, '', `#/${DEFAULT_ROUTE}`);
    return render();
  }

  // Cancela las peticiones de la vista anterior.
  controller?.abort();
  controller = new AbortController();
  const { signal } = controller;

  updateChrome(route);

  try {
    const module = route.load ? await route.load() : await import('./views/pending.js');
    if (signal.aborted) return;

    view.replaceChildren();
    view.classList.remove('view--enter');
    void view.offsetWidth; // reinicia la animación de entrada
    view.classList.add('view--enter');

    await module.render(view, { route, signal });
  } catch (error) {
    if (signal.aborted) return;
    console.error(error);
    view.innerHTML = pageHeader({ ...route, description: 'No se pudo cargar esta sección.' });
    toast('No se pudo cargar la sección. Recarga la página.', { type: 'error' });
  }

  // Al navegar, el foco va al título para lectores de pantalla y teclado.
  if (!isFirstRender) {
    window.scrollTo({ top: 0 });
    view.querySelector('.page-title')?.focus({ preventScroll: true });
  }
  isFirstRender = false;

  window.dispatchEvent(new CustomEvent('route:change', { detail: { route } }));
}

export function startRouter() {
  window.addEventListener('hashchange', render);
  render();
}
