// Top bar search box: jumps to any section (Ctrl+K or "/").
// Implements the ARIA combobox pattern so it works with the keyboard
// and screen readers.

import { visibleRoutes } from './routes.js';
import { navigate } from './router.js';
import { icon, escapeHtml, normalizeText as normalize } from './ui.js';

// Only the sections the user's role can open (recomputed on login and logout)
const buildIndex = () => visibleRoutes().map((route) => ({
  route,
  haystack: normalize([route.title, route.rf, route.description, ...(route.keywords ?? [])].join(' ')),
}));

// Top bar search box: filter, keyboard navigation and selection
export function initSearch() {
  const input = document.getElementById('global-search');
  const list = document.getElementById('search-results');
  let matches = [];
  let active = -1;

  // DOM id of a result option
  const optionId = (route) => `search-opt-${route.path}`;

  // Paints the result list
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

  // Filters the sections with the typed text and opens the list
  function open() {
    const query = normalize(input.value);
    const index = buildIndex();
    matches = (query ? index.filter((item) => item.haystack.includes(query)) : index).map((item) => item.route);
    active = matches.length ? 0 : -1;
    renderList();
    list.hidden = false;
    input.setAttribute('aria-expanded', 'true');
  }

  // Closes the result list
  function close() {
    list.hidden = true;
    input.setAttribute('aria-expanded', 'false');
    input.removeAttribute('aria-activedescendant');
  }

  // Opens the chosen section
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

  // mousedown + preventDefault: the input keeps the focus until the click.
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
