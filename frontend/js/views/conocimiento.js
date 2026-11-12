// Base de conocimiento (RF02): artículos técnicos que alimentan al asistente.
// Solo los artículos activos y verificados se usan como fuentes del RAG.
// Endpoints: GET/POST /api/v1/knowledge-articles, GET/PUT /{id}, PATCH /{id}/status

import { pageHeader, icon, escapeHtml, toast, setBusy } from '../ui.js';
import { createDataTable } from '../components/data-table.js';
import { openModal, confirmDialog } from '../components/modal.js';
import { errorState } from '../components/states.js';
import { searchArticles, createArticle, updateArticle, updateArticleStatus } from '../services/knowledge.service.js';
import { listCategories, listUsers } from '../services/catalogs.service.js';
import { formatDateTime, parseDate } from '../format.js';

const activeBadge = (a) => (a.active
  ? '<span class="badge badge--success"><i class="badge__dot" aria-hidden="true"></i>Activo</span>'
  : '<span class="badge badge--neutral"><i class="badge__dot" aria-hidden="true"></i>Inactivo</span>');

const verifiedBadge = (a) => (a.verified
  ? '<span class="badge badge--info"><i class="badge__dot" aria-hidden="true"></i>Verificado</span>'
  : '<span class="badge badge--warning"><i class="badge__dot" aria-hidden="true"></i>Sin verificar</span>');

const usable = (a) => a.active && a.verified;

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
  referenceUrl: (v) => (!v || /^https?:\/\/.+/.test(v) ? (v.length > 500 ? 'Máximo 500 caracteres.' : '') : 'Debe empezar por http:// o https://'),
  content: (v) => (!v ? 'El contenido es obligatorio.' : v.length < 20 ? 'Escribe al menos 20 caracteres.' : ''),
};

const options = (items, placeholder, selected) =>
  `<option value="">${escapeHtml(placeholder)}</option>${items.map((i) => `<option value="${i.id}" ${String(i.id) === String(selected ?? '') ? 'selected' : ''}>${escapeHtml(i.name)}</option>`).join('')}`;

export async function render(container, { route, signal }) {
  container.innerHTML = `
    <div class="page-header-row">
      ${pageHeader({ eyebrow: route.rf, title: route.title, description: route.description })}
      <button class="btn btn--primary" type="button" data-action="create" disabled>${icon('i-plus')}Nuevo artículo</button>
    </div>

    <div class="stack stack--tight">
      <form class="card filters" id="kb-filters" aria-label="Filtros de artículos">
        <div class="filters__grid">
          <div class="field"><label for="kb-q">Buscar en el contenido</label>
            <input class="input" id="kb-q" name="q" type="search" placeholder="Ej.: VPN, spooler, Kerberos" autocomplete="off"></div>
          <div class="field"><label for="kb-category">Categoría</label><select class="input" id="kb-category" name="categoryId"><option value="">Todas</option></select></div>
          <div class="field"><label for="kb-active">Estado</label>
            <select class="input" id="kb-active" name="active"><option value="">Todos</option><option value="true">Activos</option><option value="false">Inactivos</option></select></div>
          <div class="field"><label for="kb-verified">Verificación</label>
            <select class="input" id="kb-verified" name="verified"><option value="">Todos</option><option value="true">Verificados</option><option value="false">Sin verificar</option></select></div>
        </div>
        <div class="form__actions">
          <button class="btn btn--primary" type="submit">${icon('i-search')}Buscar</button>
          <button class="btn btn--ghost" type="reset">Limpiar</button>
        </div>
        <div id="kb-stats"></div>
      </form>

      <p class="note">${icon('i-info')}<span>El asistente IA solo usa como fuente los artículos <strong>activos y verificados</strong>.
        Si editas el título o el contenido de un artículo, vuelve a quedar sin verificar.</span></p>

      <section class="card" id="kb-table-card" aria-label="Artículos de la base de conocimiento"><div id="kb-table"></div></section>
    </div>`;

  const form = container.querySelector('#kb-filters');
  const stats = container.querySelector('#kb-stats');
  const createButton = container.querySelector('[data-action="create"]');
  let articles = [];
  let categories = [];
  let authors = [];

  const table = createDataTable(container.querySelector('#kb-table'), {
    caption: 'Artículos de la base de conocimiento',
    searchPlaceholder: 'Filtrar resultados…',
    emptyMessage: 'No hay artículos con estos filtros.',
    pageSize: 10,
    initialSort: { key: 'updatedAt', dir: 'desc' },
    columns: [
      { key: 'id', label: 'ID', align: 'right', render: (a) => `<span class="cell-id">#${a.id}</span>`, text: (a) => String(a.id) },
      { key: 'title', label: 'Título', render: (a) => `<span class="cell-title" title="${escapeHtml(a.title)}">${escapeHtml(a.title)}</span>` },
      { key: 'categoryName', label: 'Categoría', render: (a) => `<span class="cell-muted">${escapeHtml(a.categoryName)}</span>` },
      { key: 'active', label: 'Estado', render: activeBadge, text: (a) => (a.active ? 'Activo' : 'Inactivo'), sortValue: (a) => Number(a.active) },
      { key: 'verified', label: 'Verificación', render: verifiedBadge, text: (a) => (a.verified ? 'Verificado' : 'Sin verificar'), sortValue: (a) => Number(a.verified) },
      {
        key: 'usable', label: 'Fuente IA', render: (a) => (usable(a) ? `<span class="kb-usable">${icon('i-check')}<span class="sr-only">Sí</span></span>` : '<span class="cell-muted">No</span>'),
        text: (a) => (usable(a) ? 'Sí' : 'No'), sortValue: (a) => Number(usable(a)),
      },
      {
        key: 'updatedAt', label: 'Actualizado', render: (a) => `<span class="cell-muted">${formatDateTime(a.updatedAt)}</span>`,
        text: (a) => formatDateTime(a.updatedAt), sortValue: (a) => parseDate(a.updatedAt),
      },
    ],
    onRowClick: (article) => openDetail(article),
  });

  const showStats = () => {
    const count = (fn) => articles.filter(fn).length;
    stats.innerHTML = `
      <dl class="history-stats">
        <div><dt>Artículos</dt><dd>${articles.length}</dd></div>
        <div><dt>Usables por la IA</dt><dd>${count(usable)}</dd></div>
        <div><dt>Sin verificar</dt><dd>${count((a) => !a.verified)}</dd></div>
        <div><dt>Inactivos</dt><dd>${count((a) => !a.active)}</dd></div>
      </dl>`;
  };

  const replace = (updated) => {
    articles = articles.some((a) => a.id === updated.id)
      ? articles.map((a) => (a.id === updated.id ? updated : a))
      : [updated, ...articles];
    table.setRows(articles);
    showStats();
  };

  async function load() {
    table.setLoading(true);
    try {
      articles = await searchArticles(Object.fromEntries(new FormData(form)), { signal });
    } catch (error) {
      if (signal.aborted) return;
      errorState(container.querySelector('#kb-table-card'), error, () => render(container, { route, signal }));
      return;
    }
    if (signal.aborted) return;
    table.setLoading(false);
    table.setRows(articles);
    showStats();
  }

  form.addEventListener('submit', (event) => { event.preventDefault(); load(); });
  form.addEventListener('reset', () => setTimeout(load));
  ['categoryId', 'active', 'verified'].forEach((name) => form.elements[name].addEventListener('change', load));
  createButton.addEventListener('click', () => openEditor());

  try {
    const [cats, users] = await Promise.all([listCategories({}, { signal }), listUsers({}, { signal })]);
    categories = cats;
    // Redactan artículos los técnicos y administradores.
    authors = users.filter((u) => u.role !== 'EMPLOYEE').map((u) => ({ id: u.id, name: u.fullName }));
  } catch (error) {
    if (signal.aborted) return;
    errorState(container.querySelector('#kb-table-card'), error, () => render(container, { route, signal }));
    return;
  }
  if (signal.aborted) return;
  form.elements.categoryId.insertAdjacentHTML('beforeend', categories.map((c) => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join(''));
  createButton.disabled = false;
  await load();

  /* ---------- Detalle ---------- */
  function openDetail(article) {
    const url = safeUrl(article.referenceUrl);
    const modal = openModal({
      title: `Artículo #${article.id}`,
      size: 'lg',
      body: `
        <h3 class="detail-title">${escapeHtml(article.title)}</h3>
        <div class="cluster">${activeBadge(article)}${verifiedBadge(article)}
          ${usable(article) ? '<span class="badge badge--accent"><i class="badge__dot" aria-hidden="true"></i>Fuente del asistente</span>' : ''}</div>
        <dl class="kv kv--detail">
          <dt>Categoría</dt><dd>${escapeHtml(article.categoryName)}</dd>
          <dt>Autor</dt><dd>${escapeHtml(article.authorFullName ?? '—')}</dd>
          <dt>Creado</dt><dd>${formatDateTime(article.createdAt)}</dd>
          <dt>Actualizado</dt><dd>${formatDateTime(article.updatedAt)}</dd>
          <dt>Referencia</dt><dd>${url ? `<a class="ai-source__link" href="${escapeHtml(url)}" target="_blank" rel="noopener noreferrer">${escapeHtml(url)}${icon('i-external')}</a>` : '—'}</dd>
        </dl>
        <h4 class="detail-label">Contenido</h4>
        <p class="detail-text kb-content">${escapeHtml(article.content)}</p>
        <div class="detail-links">
          <button class="btn btn--primary" type="button" data-act="edit">${icon('i-settings')}Editar</button>
          <button class="btn btn--ghost" type="button" data-act="verify">${icon('i-check-circle')}${article.verified ? 'Quitar verificación' : 'Marcar como verificado'}</button>
          <button class="btn ${article.active ? 'btn--danger' : 'btn--ghost'}" type="button" data-act="active">${article.active ? 'Desactivar' : 'Activar'}</button>
        </div>`,
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

    modal.body.querySelector('[data-act="edit"]').addEventListener('click', () => {
      modal.close();
      openEditor(article);
    });
    modal.body.querySelector('[data-act="verify"]').addEventListener('click', (e) =>
      run(e.currentTarget, { verified: !article.verified }, article.verified ? 'El artículo quedó sin verificar.' : 'Artículo verificado: ya puede usarlo el asistente si está activo.'));
    modal.body.querySelector('[data-act="active"]').addEventListener('click', async (e) => {
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
    // Al crear solo se ofrecen categorías activas; al editar se conserva la actual.
    const categoryOptions = categories.filter((c) => c.active || c.id === article?.categoryId);
    const modal = openModal({
      title: editing ? `Editar artículo #${article.id}` : 'Nuevo artículo',
      size: 'lg',
      body: `
        <form class="form" id="kb-form" novalidate>
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
          <div class="field">
            <label for="k-url">URL de referencia (opcional)</label>
            <input class="input" id="k-url" name="referenceUrl" type="url" inputmode="url" maxlength="500" spellcheck="false" aria-describedby="k-referenceUrl-error"
              value="${escapeHtml(article?.referenceUrl ?? '')}" placeholder="https://…">
            <p class="field__error" id="k-referenceUrl-error" aria-live="polite"></p>
          </div>
          <div class="field">
            <label for="k-content">Contenido</label>
            <textarea class="input input--area" id="k-content" name="content" rows="8" aria-describedby="k-content-hint k-content-error"
              placeholder="Problema, síntomas, causa y pasos de solución">${escapeHtml(article?.content ?? '')}</textarea>
            <p class="field__hint" id="k-content-hint">${editing && article.verified
              ? 'Este artículo está verificado. Si cambias el título o el contenido, tendrá que verificarse de nuevo.'
              : 'Describe el problema, los síntomas, la causa y los pasos de solución. El artículo nace sin verificar.'}</p>
            <p class="field__error" id="k-content-error" aria-live="polite"></p>
          </div>
          <div class="form__actions">
            <button class="btn btn--primary" type="submit">${icon('i-check')}${editing ? 'Guardar cambios' : 'Crear artículo'}</button>
            <button class="btn btn--ghost" type="button" data-cancel>Cancelar</button>
          </div>
        </form>`,
    });

    const editor = modal.body.querySelector('#kb-form');
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
    modal.body.querySelector('[data-cancel]').addEventListener('click', () => modal.close());

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
        toast(error.message, { type: 'error' });
      }
    });
  }
}
