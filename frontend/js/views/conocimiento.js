// Base de conocimiento (RF02): artículos técnicos que alimentan al asistente.
// Solo los artículos activos y verificados se usan como fuentes del RAG.
// Endpoints: GET/POST /api/v1/knowledge-articles, GET/PUT /{id}, PATCH /{id}/status
//
// Según el rol:
// - Administrador: crea, edita, verifica y activa o desactiva artículos.
// - Técnico: crea y edita; la verificación la hace un administrador.
// - Usuario (EMPLOYEE): solo lee los artículos publicados (activos y verificados).

import { pageHeader, icon, escapeHtml, toast, setBusy, avatar } from '../ui.js';
import { createDataTable } from '../components/data-table.js';
import { openModal, confirmDialog } from '../components/modal.js';
import { errorState, emptyHtml } from '../components/states.js';
import { skeletonLines } from '../components/skeleton.js';
import { searchArticles, createArticle, updateArticle, updateArticleStatus } from '../services/knowledge.service.js';
import { listCategories, listUsers } from '../services/catalogs.service.js';
import { formatDate, formatDateTime, parseDate } from '../format.js';
import { showServerFieldErrors } from '../components/form-errors.js';
import { isAdmin, isEmployee } from '../auth.js';

const VIEW_KEY = 'incidentes-ia:kb-view';

// Estado visual de un artículo: usable por la IA, pendiente de verificación o inactivo.
function stateOf(a) {
  if (!a.active) return { key: 'inactive', label: 'Inactivo', tone: 'neutral' };
  if (!a.verified) return { key: 'pending', label: 'Pendiente de verificación', tone: 'warning' };
  return { key: 'ok', label: 'Activo y verificado', tone: 'success' };
}
const stateBadge = (a) => {
  const s = stateOf(a);
  return `<span class="badge badge--${s.tone}"><i class="badge__dot" aria-hidden="true"></i>${s.label}</span>`;
};

// Artículos anteriores a las secciones solo tienen "content".
const hasSections = (a) => Boolean(a.problem || a.solution);

const safeUrl = (value) => {
  try {
    const url = new URL(value);
    return ['http:', 'https:'].includes(url.protocol) ? url.href : null;
  } catch {
    return null;
  }
};

// Reglas de KnowledgeArticleRequestDTO, validadas también aquí.
const RULES = {
  title: (v) => (!v ? 'El título es obligatorio.' : v.length < 5 || v.length > 180 ? 'Debe tener entre 5 y 180 caracteres.' : ''),
  categoryId: (v) => (v ? '' : 'Elige una categoría.'),
  problem: (v) => (!v ? 'Describe el problema.' : v.length < 10 ? 'Escribe al menos 10 caracteres.' : v.length > 4000 ? 'Máximo 4000 caracteres.' : ''),
  symptoms: (v) => (v.length > 4000 ? 'Máximo 4000 caracteres.' : ''),
  cause: (v) => (v.length > 4000 ? 'Máximo 4000 caracteres.' : ''),
  solution: (v) => (!v ? 'Describe la solución.' : v.length < 10 ? 'Escribe al menos 10 caracteres.' : v.length > 8000 ? 'Máximo 8000 caracteres.' : ''),
  referenceUrl: (v) => (!v ? '' : !/^https?:\/\/.+/.test(v) ? 'Debe empezar por http:// o https://' : v.length > 500 ? 'Máximo 500 caracteres.' : ''),
};

const SECTIONS = [
  { name: 'problem', label: 'Problema', rows: 3, required: true, max: 4000, placeholder: 'Qué ocurre y a quién afecta' },
  { name: 'symptoms', label: 'Síntomas', rows: 3, max: 4000, placeholder: 'Mensajes de error, comportamiento observado (opcional)' },
  { name: 'cause', label: 'Causa', rows: 2, max: 4000, placeholder: 'Causa raíz identificada (opcional)' },
  { name: 'solution', label: 'Solución', rows: 5, required: true, max: 8000, placeholder: 'Pasos para resolverlo, uno por línea' },
];

const options = (items, placeholder, selected) =>
  `<option value="">${escapeHtml(placeholder)}</option>${items.map((i) => `<option value="${i.id}" ${String(i.id) === String(selected ?? '') ? 'selected' : ''}>${escapeHtml(i.name)}</option>`).join('')}`;

const readView = () => {
  try { return localStorage.getItem(VIEW_KEY) === 'table' ? 'table' : 'cards'; } catch { return 'cards'; }
};
const saveView = (view) => {
  try { localStorage.setItem(VIEW_KEY, view); } catch { /* sin almacenamiento: se usa el valor por defecto */ }
};

function cardHtml(a, { canEdit }) {
  const s = stateOf(a);
  const summary = a.problem || a.content;
  return `
    <li>
      <article class="kb-card kb-card--${s.key}">
        <header class="kb-card__head">
          <span class="kb-card__category">${escapeHtml(a.categoryName)}</span>
          ${stateBadge(a)}
        </header>
        <h3 class="kb-card__title">${escapeHtml(a.title)}</h3>
        <p class="kb-card__text">${escapeHtml(summary)}</p>
        ${a.symptoms ? `<p class="kb-card__meta"><strong>Síntomas:</strong> ${escapeHtml(a.symptoms)}</p>` : ''}
        <footer class="kb-card__foot">
          <span class="person cell-muted">${a.authorFullName ? avatar(a.authorFullName) : ''}#${a.id} · ${formatDate(a.updatedAt)}${a.authorFullName ? ` · ${escapeHtml(a.authorFullName)}` : ''}</span>
          <span class="row-actions">
            <button class="icon-btn icon-btn--sm" type="button" data-kb-action="view" data-id="${a.id}" aria-label="Ver artículo #${a.id}" title="Ver">${icon('i-eye')}</button>
            ${canEdit ? `<button class="icon-btn icon-btn--sm" type="button" data-kb-action="edit" data-id="${a.id}" aria-label="Editar artículo #${a.id}" title="Editar">${icon('i-settings')}</button>` : ''}
          </span>
        </footer>
      </article>
    </li>`;
}

export async function render(container, { route, signal }) {
  const reader = isEmployee();      // solo lectura de artículos publicados
  const canEdit = !reader;          // administrador y técnico redactan
  const canPublish = isAdmin();     // verificar y activar: solo administrador

  container.innerHTML = `
    <div class="page-header-row">
      ${pageHeader(route)}
      ${canEdit ? `<button class="btn btn--primary" type="button" data-action="create" disabled>${icon('i-plus')}Nuevo artículo</button>` : ''}
    </div>

    <div class="stack stack--tight">
      <form class="card filters" id="kb-filters" aria-label="Filtros de artículos">
        <div class="filters__grid">
          <div class="field"><label for="kb-q">Buscar</label>
            <input class="input" id="kb-q" name="q" type="search" placeholder="Ej.: VPN, spooler, Kerberos" autocomplete="off"></div>
          <div class="field"><label for="kb-category">Categoría</label><select class="input" id="kb-category" name="categoryId"><option value="">Todas</option></select></div>
          ${reader ? '' : `<div class="field"><label for="kb-active">Estado</label>
            <select class="input" id="kb-active" name="active"><option value="">Todos</option><option value="true">Activos</option><option value="false">Inactivos</option></select></div>
          <div class="field"><label for="kb-verified">Verificación</label>
            <select class="input" id="kb-verified" name="verified"><option value="">Todos</option><option value="true">Verificados</option><option value="false">Pendientes de verificación</option></select></div>`}
        </div>
        <div class="form__actions">
          <button class="btn btn--primary" type="submit">${icon('i-search')}Buscar</button>
          <button class="btn btn--ghost" type="reset">Limpiar</button>
        </div>
        <div id="kb-stats"></div>
      </form>

      <div class="kb-toolbar">
        ${reader ? `<ul class="kb-legend"><li><i class="kb-dot kb-dot--ok" aria-hidden="true"></i>Guías verificadas por el equipo de soporte</li></ul>` : `<ul class="kb-legend" aria-label="Leyenda de estados">
          <li><i class="kb-dot kb-dot--ok" aria-hidden="true"></i>Activo y verificado: lo usa el asistente</li>
          <li><i class="kb-dot kb-dot--pending" aria-hidden="true"></i>Pendiente de verificación</li>
          <li><i class="kb-dot kb-dot--inactive" aria-hidden="true"></i>Inactivo</li>
        </ul>`}
        <div class="segmented-toggle" role="group" aria-label="Vista">
          <button class="btn btn--ghost" type="button" data-view="cards" aria-pressed="false">${icon('i-layers')}Tarjetas</button>
          <button class="btn btn--ghost" type="button" data-view="table" aria-pressed="false">${icon('i-dashboard')}Tabla</button>
        </div>
      </div>

      <section id="kb-results" aria-live="polite" aria-label="Artículos de la base de conocimiento">
        <div id="kb-cards">${skeletonLines(4)}</div>
        <div class="card" id="kb-table-card" hidden><div id="kb-table"></div></div>
      </section>
    </div>`;

  const form = container.querySelector('#kb-filters');
  const stats = container.querySelector('#kb-stats');
  const createButton = container.querySelector('[data-action="create"]');
  const cardsEl = container.querySelector('#kb-cards');
  const tableCard = container.querySelector('#kb-table-card');
  let articles = [];
  let categories = [];
  let authors = [];
  let view = readView();

  const table = createDataTable(container.querySelector('#kb-table'), {
    caption: 'Artículos de la base de conocimiento',
    searchPlaceholder: 'Filtrar resultados…',
    emptyMessage: 'No hay artículos con estos filtros.',
    pageSize: 10,
    initialSort: { key: 'updatedAt', dir: 'desc' },
    columns: [
      { key: 'id', label: 'ID', align: 'right', render: (a) => `<span class="cell-id">#${a.id}</span>`, text: (a) => String(a.id) },
      { key: 'title', label: 'Título', render: (a) => `<span class="cell-title" title="${escapeHtml(a.title)}">${escapeHtml(a.title)}</span>` },
      {
        key: 'problem', label: 'Problema', render: (a) => `<span class="cell-muted cell-clip" title="${escapeHtml(a.problem || a.content)}">${escapeHtml(a.problem || a.content)}</span>`,
        text: (a) => `${a.problem ?? ''} ${a.symptoms ?? ''} ${a.cause ?? ''} ${a.solution ?? ''} ${a.content}`,
      },
      { key: 'categoryName', label: 'Categoría', render: (a) => `<span class="cell-muted">${escapeHtml(a.categoryName)}</span>` },
      { key: 'state', label: 'Estado', render: stateBadge, text: (a) => stateOf(a).label, sortValue: (a) => ['ok', 'pending', 'inactive'].indexOf(stateOf(a).key) },
      {
        key: 'updatedAt', label: 'Actualizado', render: (a) => `<span class="cell-muted">${formatDateTime(a.updatedAt)}</span>`,
        text: (a) => formatDateTime(a.updatedAt), sortValue: (a) => parseDate(a.updatedAt),
      },
    ],
    onRowClick: (article) => openDetail(article),
  });

  const showStats = () => {
    const count = (key) => articles.filter((a) => stateOf(a).key === key).length;
    if (reader) {
      stats.innerHTML = `<dl class="history-stats"><div><dt>Guías disponibles</dt><dd>${articles.length}</dd></div></dl>`;
      return;
    }
    stats.innerHTML = `
      <dl class="history-stats">
        <div><dt>Artículos</dt><dd>${articles.length}</dd></div>
        <div><dt>Usables por la IA</dt><dd>${count('ok')}</dd></div>
        <div><dt>Pendientes de verificación</dt><dd>${count('pending')}</dd></div>
        <div><dt>Inactivos</dt><dd>${count('inactive')}</dd></div>
      </dl>`;
  };

  const showResults = () => {
    container.querySelectorAll('[data-view]').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.view === view)));
    tableCard.hidden = view !== 'table';
    cardsEl.hidden = view !== 'cards';
    if (view === 'table') {
      table.setRows(articles);
    } else {
      const sorted = [...articles].sort((a, b) => (parseDate(b.updatedAt) ?? 0) - (parseDate(a.updatedAt) ?? 0));
      cardsEl.innerHTML = sorted.length
        ? `<ul class="kb-grid">${sorted.map((a) => cardHtml(a, { canEdit })).join('')}</ul>`
        : `<div class="card">${emptyHtml('Sin artículos', 'No hay artículos con estos filtros.', 'i-book')}</div>`;
    }
    showStats();
  };

  const replace = (updated) => {
    articles = articles.some((a) => a.id === updated.id)
      ? articles.map((a) => (a.id === updated.id ? updated : a))
      : [updated, ...articles];
    showResults();
  };

  async function load() {
    if (view === 'table') table.setLoading(true);
    else cardsEl.innerHTML = skeletonLines(4);
    try {
      articles = await searchArticles(Object.fromEntries(new FormData(form)), { signal });
    } catch (error) {
      if (signal.aborted) return;
      errorState(container.querySelector('#kb-results'), error, () => render(container, { route, signal }));
      return;
    }
    if (signal.aborted) return;
    table.setLoading(false);
    showResults();
  }

  form.addEventListener('submit', (event) => { event.preventDefault(); load(); });
  form.addEventListener('reset', () => setTimeout(load));
  ['categoryId', 'active', 'verified'].forEach((name) => form.elements[name]?.addEventListener('change', load));
  createButton?.addEventListener('click', () => openEditor());
  container.querySelectorAll('[data-view]').forEach((button) => button.addEventListener('click', () => {
    view = button.dataset.view;
    saveView(view);
    showResults();
  }));
  cardsEl.addEventListener('click', (event) => {
    const button = event.target.closest('[data-kb-action]');
    if (!button) return;
    const article = articles.find((a) => String(a.id) === button.dataset.id);
    if (!article) return;
    if (button.dataset.kbAction === 'edit' && canEdit) openEditor(article);
    else openDetail(article);
  });

  try {
    // El directorio de usuarios (autores) solo lo necesita quien redacta
    const [cats, users] = await Promise.all([listCategories({}, { signal }), canEdit ? listUsers({}, { signal }) : []]);
    categories = cats;
    // Redactan artículos los técnicos y administradores.
    authors = users.filter((u) => u.role !== 'EMPLOYEE').map((u) => ({ id: u.id, name: u.fullName }));
  } catch (error) {
    if (signal.aborted) return;
    errorState(container.querySelector('#kb-results'), error, () => render(container, { route, signal }));
    return;
  }
  if (signal.aborted) return;
  form.elements.categoryId.insertAdjacentHTML('beforeend', categories.map((c) => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join(''));
  if (createButton) createButton.disabled = false;
  await load();

  /* ---------- Detalle ---------- */
  function openDetail(article) {
    const url = safeUrl(article.referenceUrl);
    const section = (label, text) => (text ? `<h4 class="detail-label">${label}</h4><p class="detail-text kb-content">${escapeHtml(text)}</p>` : '');
    const modal = openModal({
      title: `Artículo #${article.id}`,
      size: 'lg',
      body: `
        <h3 class="detail-title">${escapeHtml(article.title)}</h3>
        <div class="cluster">${stateBadge(article)}</div>
        <dl class="kv kv--detail">
          <dt>Categoría</dt><dd>${escapeHtml(article.categoryName)}</dd>
          <dt>Autor</dt><dd>${escapeHtml(article.authorFullName ?? '—')}</dd>
          <dt>Creado</dt><dd>${formatDateTime(article.createdAt)}</dd>
          <dt>Actualizado</dt><dd>${formatDateTime(article.updatedAt)}</dd>
          <dt>Referencia</dt><dd>${url ? `<a class="ai-source__link" href="${escapeHtml(url)}" target="_blank" rel="noopener noreferrer">${escapeHtml(url)}${icon('i-external')}</a>` : '—'}</dd>
        </dl>
        ${hasSections(article)
          ? section('Problema', article.problem) + section('Síntomas', article.symptoms) + section('Causa', article.cause) + section('Solución', article.solution)
          : `${section('Contenido', article.content)}<p class="note note--warning">${icon('i-info')}<span>Este artículo es anterior a las secciones Problema, Síntomas, Causa y Solución. Edítalo para organizarlo.</span></p>`}
        ${canEdit ? `<div class="detail-links">
          <button class="btn btn--primary" type="button" data-act="edit">${icon('i-settings')}Editar</button>
          ${canPublish ? `
          <button class="btn btn--ghost" type="button" data-act="verify">${icon('i-check-circle')}${article.verified ? 'Quitar verificación' : 'Marcar como verificado'}</button>
          <button class="btn ${article.active ? 'btn--danger' : 'btn--ghost'}" type="button" data-act="active">${article.active ? 'Desactivar' : 'Activar'}</button>`
          : `<p class="field__hint">Un administrador verifica el artículo antes de que lo use el asistente.</p>`}
        </div>` : ''}`,
    });

    const run = async (button, changes, message) => {
      setBusy(button, true, 'Guardando…');
      try {
        const updated = await updateArticleStatus(article.id, changes);
        replace(updated);
        toast(message, { type: 'success' });
        modal.close();
      } catch (error) {
        setBusy(button, false);
        toast(error.message, { type: 'error' });
      }
    };

    modal.body.querySelector('[data-act="edit"]')?.addEventListener('click', () => {
      modal.close();
      openEditor(article);
    });
    modal.body.querySelector('[data-act="verify"]')?.addEventListener('click', (e) =>
      run(e.currentTarget, { verified: !article.verified }, article.verified
        ? 'El artículo quedó pendiente de verificación.'
        : 'Artículo verificado: el asistente puede usarlo si está activo.'));
    modal.body.querySelector('[data-act="active"]')?.addEventListener('click', async (e) => {
      const button = e.currentTarget;
      if (article.active) {
        const ok = await confirmDialog({
          title: 'Desactivar artículo',
          message: 'El asistente dejará de usarlo como fuente. Podrás activarlo de nuevo cuando quieras.',
          confirmLabel: 'Desactivar',
          danger: true,
        });
        if (!ok) return;
      }
      run(button, { active: !article.active }, article.active ? 'Artículo desactivado.' : 'Artículo activado.');
    });
  }

  /* ---------- Crear / editar ---------- */
  function openEditor(article = null) {
    const editing = Boolean(article);
    const legacy = editing && !hasSections(article);
    // Al crear solo se ofrecen categorías activas; al editar se conserva la actual.
    const categoryOptions = categories.filter((c) => c.active || c.id === article?.categoryId);
    // Un artículo antiguo trae todo en "content": se precarga como problema para reorganizarlo.
    const initial = (name) => (legacy && name === 'problem' ? article.content : article?.[name] ?? '');

    const modal = openModal({
      title: editing ? `Editar artículo #${article.id}` : 'Nuevo artículo',
      size: 'lg',
      body: `
        <form class="form" id="kb-form" novalidate>
          ${legacy ? `<p class="note note--warning">${icon('i-info')}<span>Este artículo tenía todo el texto junto. Lo cargamos en <strong>Problema</strong>: separa la solución en su campo.</span></p>` : ''}
          <div class="field">
            <label for="k-title">Título</label>
            <input class="input" id="k-title" name="title" maxlength="180" autocomplete="off" aria-describedby="k-title-error"
              value="${escapeHtml(article?.title ?? '')}" placeholder="Ej.: La cola de impresión se queda atascada">
            <p class="field__error" id="k-title-error" aria-live="polite"></p>
          </div>
          <div class="form-row">
            <div class="field">
              <label for="k-category">Categoría</label>
              <select class="input" id="k-category" name="categoryId" aria-describedby="k-categoryId-error">${options(categoryOptions, 'Selecciona…', article?.categoryId)}</select>
              <p class="field__error" id="k-categoryId-error" aria-live="polite"></p>
            </div>
            <div class="field">
              <label for="k-author">Autor</label>
              <select class="input" id="k-author" name="authorUserId">${options(authors, editing ? 'Sin cambios' : 'Sin autor', article?.authorUserId)}</select>
            </div>
          </div>
          ${SECTIONS.map((sec) => `
            <div class="field">
              <label for="k-${sec.name}">${sec.label}${sec.required ? '' : ' <span class="cell-muted">(opcional)</span>'}</label>
              <textarea class="input input--area" id="k-${sec.name}" name="${sec.name}" rows="${sec.rows}" maxlength="${sec.max}" aria-describedby="k-${sec.name}-error"
                placeholder="${escapeHtml(sec.placeholder)}">${escapeHtml(initial(sec.name))}</textarea>
              <p class="field__error" id="k-${sec.name}-error" aria-live="polite"></p>
            </div>`).join('')}
          <div class="field">
            <label for="k-url">URL de referencia <span class="cell-muted">(opcional)</span></label>
            <input class="input" id="k-url" name="referenceUrl" type="url" inputmode="url" maxlength="500" spellcheck="false" aria-describedby="k-referenceUrl-error"
              value="${escapeHtml(article?.referenceUrl ?? '')}" placeholder="https://…">
            <p class="field__error" id="k-referenceUrl-error" aria-live="polite"></p>
          </div>
          <p class="field__hint">${editing && article.verified
            ? 'Este artículo está verificado. Si cambias el título o el contenido, tendrá que verificarse de nuevo.'
            : 'Los artículos nuevos quedan pendientes de verificación hasta que un especialista los revise.'}</p>
          <div class="form__actions">
            <button class="btn btn--primary" type="submit">${icon('i-check')}${editing ? 'Guardar cambios' : 'Crear artículo'}</button>
            <button class="btn btn--ghost" type="button" data-cancel>Cancelar</button>
          </div>
        </form>`,
    });

    const editor = modal.body.querySelector('#kb-form');
    const initialData = JSON.stringify(Object.fromEntries(new FormData(editor)));
    const touched = new Set();
    const validateField = (name) => {
      const field = editor.elements[name];
      const message = RULES[name](field.value.trim());
      editor.querySelector(`#k-${name}-error`).textContent = message;
      field.setAttribute('aria-invalid', String(Boolean(message)));
      return !message;
    };
    Object.keys(RULES).forEach((name) => {
      const field = editor.elements[name];
      field.addEventListener('blur', () => { touched.add(name); validateField(name); });
      field.addEventListener('input', () => { if (touched.has(name)) validateField(name); });
      field.addEventListener('change', () => { if (touched.has(name)) validateField(name); });
    });

    modal.body.querySelector('[data-cancel]').addEventListener('click', async () => {
      const dirty = JSON.stringify(Object.fromEntries(new FormData(editor))) !== initialData;
      if (dirty && !(await confirmDialog({ title: 'Descartar cambios', message: 'Se perderá lo que escribiste.', confirmLabel: 'Descartar', danger: true }))) return;
      modal.close();
    });

    editor.addEventListener('submit', async (event) => {
      event.preventDefault();
      const results = Object.keys(RULES).map((name) => validateField(name));
      if (results.includes(false)) {
        editor.querySelector('[aria-invalid="true"]')?.focus();
        return;
      }
      const button = editor.querySelector('[type="submit"]');
      setBusy(button, true, 'Guardando…');
      const data = Object.fromEntries(new FormData(editor));
      try {
        const saved = editing ? await updateArticle(article.id, data) : await createArticle(data);
        replace(saved);
        modal.close();
        const reverified = editing && article.verified && !saved.verified;
        toast(editing
          ? (reverified ? 'Cambios guardados. El artículo quedó pendiente de verificación.' : 'Cambios guardados.')
          : `Artículo #${saved.id} creado. Verifícalo para que lo use el asistente.`, { type: reverified ? 'warning' : 'success' });
      } catch (error) {
        setBusy(button, false);
        const shown = showServerFieldErrors(editor, error, (name) => `k-${name}-error`);
        toast(shown ? 'Revisa los campos marcados.' : error.message, { type: 'error' });
      }
    });
  }
}
