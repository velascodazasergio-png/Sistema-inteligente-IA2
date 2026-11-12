// Buscador de la topbar: salta a cualquier sección (Ctrl+K o "/").
// Implementa el patrón combobox de ARIA para que funcione con teclado
// y lectores de pantalla.

import { routes } from './routes.js';
import { navigate } from './router.js';
import { icon, escapeHtml, normalizeText as normalize } from './ui.js';

const index = routes.map((route) => ({
  route,
  haystack: normalize([route.title, route.rf, route.description, ...(route.keywords ?? [])].join(' ')),
}));

export function initSearch() {
  const input = document.getElementById('global-search');
  const list = document.getElementById('search-results');
  let matches = [];
  let active = -1;

  const optionId = (route) => `search-opt-${route.path}`;

  function renderList() {
    list.innerHTML = matches.length
      ? matches.map((route, i) => `
          <li id="${optionId(route)}" class="search__option" role="option"
              aria-selected="${i === active}" data-path="${route.path}">
            ${icon(route.icon)}<span>${escapeHtml(route.title)}</span>
            ${route.rf ? `<small>${route.rf}</small>` : ''}
          </li>`).join('')
      : `<li class="search__empty" role="presentation">Sin coincidencias para “${escapeHtml(input.value)}”</li>`;

    if (active >= 0) input.setAttribute('aria-activedescendant', optionId(matches[active]));
    else input.removeAttribute('aria-activedescendant');
  }

  function open() {
    const query = normalize(input.value);
    matches = query ? index.filter((item) => item.haystack.includes(query)).map((item) => item.route) : routes;
    active = matches.length ? 0 : -1;
    renderList();
    list.hidden = false;
    input.setAttribute('aria-expanded', 'true');
  }

  function close() {
    list.hidden = true;
    input.setAttribute('aria-expanded', 'false');
    input.removeAttribute('aria-activedescendant');
  }

  function select(path) {
    input.value = '';
    close();
    input.blur();
    navigate(path);
  }

  input.addEventListener('input', open);
  input.addEventListener('focus', open);
  input.addEventListener('blur', close);

  input.addEventListener('keydown', (event) => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      if (list.hidden) open();
      if (!matches.length) return;
      const step = event.key === 'ArrowDown' ? 1 : -1;
      active = (active + step + matches.length) % matches.length;
      renderList();
      document.getElementById(optionId(matches[active]))?.scrollIntoView({ block: 'nearest' });
    } else if (event.key === 'Enter') {
      event.preventDefault();
      if (active >= 0) select(matches[active].path);
    } else if (event.key === 'Escape') {
      input.value = '';
      input.blur();
    }
  });

  // mousedown + preventDefault: el input no pierde el foco antes del clic.
  list.addEventListener('mousedown', (event) => {
    event.preventDefault();
    const option = event.target.closest('[data-path]');
    if (option) select(option.dataset.path);
  });

  document.addEventListener('keydown', (event) => {
    const typing = event.target.closest?.('input, textarea, select, [contenteditable="true"]');
    const shortcut = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k';
    if (shortcut || (event.key === '/' && !typing)) {
      event.preventDefault();
      input.focus();
      input.select();
    }
  });
}
