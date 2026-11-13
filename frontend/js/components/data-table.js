// Tabla de datos con búsqueda instantánea, ordenamiento y paginación.
// Trabaja en el cliente con las filas que recibe. Si el backend pagina
// (Spring Data Page), se puede reemplazar por paginación del servidor
// manteniendo la misma interfaz visual.

import { icon, escapeHtml, normalizeText } from '../ui.js';

let uid = 0;
const collator = new Intl.Collator('es', { sensitivity: 'base', numeric: true });

function compare(a, b) {
  if (a == null && b == null) return 0;
  if (a == null) return 1;
  if (b == null) return -1;
  if (typeof a === 'number' && typeof b === 'number') return a - b;
  if (a instanceof Date && b instanceof Date) return a - b;
  return collator.compare(String(a), String(b));
}

// Números de página compactos: 1 … 4 5 6 … 12
function pageList(page, pages) {
  if (pages <= 7) return Array.from({ length: pages }, (_, i) => i + 1);
  const set = [...new Set([1, pages, page - 1, page, page + 1])]
    .filter((p) => p >= 1 && p <= pages)
    .sort((a, b) => a - b);
  return set.flatMap((p, i) => (i && p - set[i - 1] > 1 ? ['…', p] : [p]));
}

/**
 * @param {HTMLElement} container
 * @param {object} o
 * @param {Array<{key:string,label:string,sortable?:boolean,searchable?:boolean,align?:'right',
 *   render?:(row)=>string, text?:(row)=>string, sortValue?:(row)=>any}>} o.columns
 *   render devuelve HTML: escapa con escapeHtml todo dato que venga del backend.
 * @param {Array<object>} [o.rows]
 * @param {number} [o.pageSize]
 * @param {string} o.caption Descripción de la tabla para lectores de pantalla.
 * @param {(row)=>void} [o.onRowClick]
 * @param {{key:string, dir:'asc'|'desc'}} [o.initialSort]
 */
export function createDataTable(container, {
  columns,
  rows = [],
  pageSize = 10,
  caption = '',
  searchPlaceholder = 'Buscar…',
  emptyMessage = 'No hay registros.',
  onRowClick,
  initialSort,
}) {
  const id = `dt-${++uid}`;
  const state = {
    rows,
    query: '',
    sortKey: initialSort?.key ?? null,
    sortDir: initialSort?.dir ?? 'asc',
    page: 1,
    loading: false,
  };
  let visible = [];

  container.classList.add('data-table');
  container.innerHTML = `
    <div class="data-table__toolbar">
      <div class="input-icon data-table__search">
        ${icon('i-search')}
        <label class="sr-only" for="${id}-q">${escapeHtml(searchPlaceholder)}</label>
        <input class="input" id="${id}-q" type="search" placeholder="${escapeHtml(searchPlaceholder)}" autocomplete="off">
      </div>
      <p class="data-table__count" aria-live="polite"></p>
    </div>
    <div class="table-wrap">
      <table class="table">
        <caption class="sr-only">${escapeHtml(caption)}</caption>
        <thead><tr></tr></thead>
        <tbody></tbody>
      </table>
    </div>
    <div class="data-table__footer">
      <span class="data-table__range"></span>
      <nav class="pagination" aria-label="Paginación"></nav>
    </div>`;

  const input = container.querySelector('input');
  const headRow = container.querySelector('thead tr');
  const tbody = container.querySelector('tbody');
  const countEl = container.querySelector('.data-table__count');
  const rangeEl = container.querySelector('.data-table__range');
  const nav = container.querySelector('.pagination');

  const valueOf = (col, row) => (col.sortValue ? col.sortValue(row) : row[col.key]);
  const textOf = (col, row) => (col.text ? col.text(row) : row[col.key]);

  function filtered() {
    const query = normalizeText(state.query);
    const list = query
      ? state.rows.filter((row) => columns.some((col) =>
        col.searchable !== false && normalizeText(textOf(col, row)).includes(query)))
      : [...state.rows];
    const col = columns.find((c) => c.key === state.sortKey);
    if (col) {
      const dir = state.sortDir === 'asc' ? 1 : -1;
      list.sort((a, b) => compare(valueOf(col, a), valueOf(col, b)) * dir);
    }
    return list;
  }

  function renderHead() {
    headRow.innerHTML = columns.map((col) => {
      const sorted = state.sortKey === col.key;
      const ariaSort = sorted ? ` aria-sort="${state.sortDir === 'asc' ? 'ascending' : 'descending'}"` : '';
      const align = col.align === 'right' ? ' class="num"' : '';
      const sortIcon = sorted ? (state.sortDir === 'asc' ? 'i-chevron-up' : 'i-chevron-down') : 'i-sort';
      const content = col.sortable === false
        ? escapeHtml(col.label)
        : `<button class="sort-btn" type="button" data-sort="${col.key}">${escapeHtml(col.label)}${icon(sortIcon)}</button>`;
      return `<th scope="col"${ariaSort}${align}>${content}</th>`;
    }).join('');
  }

  function renderBody() {
    if (state.loading) {
      tbody.innerHTML = Array.from({ length: 5 }, () => `
        <tr aria-hidden="true">${columns.map(() => '<td><span class="skeleton skeleton--text"></span></td>').join('')}</tr>`).join('');
      countEl.textContent = 'Cargando…';
      rangeEl.textContent = '';
      nav.innerHTML = '';
      return;
    }

    visible = filtered();
    const pages = Math.max(1, Math.ceil(visible.length / pageSize));
    state.page = Math.min(state.page, pages);
    const start = (state.page - 1) * pageSize;
    const pageRows = visible.slice(start, start + pageSize);

    tbody.innerHTML = pageRows.length
      ? pageRows.map((row, i) => `
          <tr${onRowClick ? ` data-index="${start + i}" tabindex="0" data-clickable` : ''}>
            ${columns.map((col) => `<td${col.align === 'right' ? ' class="num"' : ''}>${
              col.render ? col.render(row) : escapeHtml(row[col.key] ?? '')}</td>`).join('')}
          </tr>`).join('')
      : `<tr class="table__empty"><td colspan="${columns.length}">${escapeHtml(
        state.query ? `Sin resultados para “${state.query}”.` : emptyMessage)}</td></tr>`;

    countEl.textContent = `${visible.length} ${visible.length === 1 ? 'registro' : 'registros'}`;
    rangeEl.textContent = visible.length
      ? `Mostrando ${start + 1}–${start + pageRows.length} de ${visible.length}`
      : '';

    nav.innerHTML = pages > 1 ? `
      <button class="page-btn" type="button" data-nav="prev" aria-label="Página anterior" ${state.page === 1 ? 'disabled' : ''}>${icon('i-chevron-left')}</button>
      ${pageList(state.page, pages).map((p) => (p === '…'
        ? '<span class="page-gap" aria-hidden="true">…</span>'
        : `<button class="page-btn" type="button" data-page="${p}" aria-label="Página ${p}" ${p === state.page ? 'aria-current="page"' : ''}>${p}</button>`)).join('')}
      <button class="page-btn" type="button" data-nav="next" aria-label="Página siguiente" ${state.page === pages ? 'disabled' : ''}>${icon('i-chevron-right')}</button>` : '';
  }

  function render() {
    renderHead();
    renderBody();
  }

  let debounce;
  input.addEventListener('input', () => {
    clearTimeout(debounce);
    debounce = setTimeout(() => {
      state.query = input.value;
      state.page = 1;
      renderBody();
    }, 150);
  });

  headRow.addEventListener('click', (event) => {
    const button = event.target.closest('[data-sort]');
    if (!button) return;
    const key = button.dataset.sort;
    state.sortDir = state.sortKey === key && state.sortDir === 'asc' ? 'desc' : 'asc';
    state.sortKey = key;
    render();
    headRow.querySelector(`[data-sort="${key}"]`)?.focus();
  });

  nav.addEventListener('click', (event) => {
    const button = event.target.closest('button');
    if (!button || button.disabled) return;
    const pages = Math.max(1, Math.ceil(visible.length / pageSize));
    if (button.dataset.nav === 'prev') state.page = Math.max(1, state.page - 1);
    else if (button.dataset.nav === 'next') state.page = Math.min(pages, state.page + 1);
    else state.page = Number(button.dataset.page);
    renderBody();
    // Mantiene el foco en la paginación después de redibujar.
    const again = button.dataset.nav ? nav.querySelector(`[data-nav="${button.dataset.nav}"]:not(:disabled)`) : null;
    (again ?? nav.querySelector('[aria-current="page"]'))?.focus();
  });

  if (onRowClick) {
    const activate = (event) => {
      if (event.target.closest('button, a, input, select, textarea')) return;
      const tr = event.target.closest('tr[data-index]');
      if (tr) onRowClick(visible[Number(tr.dataset.index)]);
    };
    tbody.addEventListener('click', activate);
    tbody.addEventListener('keydown', (event) => {
      if (event.key === 'Enter' || event.key === ' ') {
        if (event.target.matches('tr[data-index]')) {
          event.preventDefault();
          activate(event);
        }
      }
    });
  }

  render();

  return {
    setRows(next) {
      state.rows = next ?? [];
      state.loading = false;
      state.page = 1;
      render();
    },
    setLoading(loading) {
      state.loading = loading;
      renderBody();
    },
  };
}
