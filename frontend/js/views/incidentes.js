// Incidentes (RF01): registro, consulta, filtrado y cambio de estado.
// Endpoints: GET/POST /api/v1/incidents, PATCH /api/v1/incidents/{id}/status

import { pageHeader, icon, escapeHtml, toast, setBusy, avatar } from '../ui.js';
import { createDataTable } from '../components/data-table.js';
import { openModal, confirmDialog } from '../components/modal.js';
import { errorState } from '../components/states.js';
import { listIncidents, getIncident, createIncident, updateIncidentStatus } from '../services/incidents.service.js';
import { listCategories, listUsers, toOptions } from '../services/catalogs.service.js';
import { ESTADO_INCIDENTE, PRIORIDAD, badge, optionsHtml, labelOf } from '../catalogs.js';
import { formatDate, formatDateTime, parseDate } from '../format.js';
import { routeParams } from '../router.js';
import { showServerFieldErrors, attachCounter } from '../components/form-errors.js';

const selectOptions = (items, placeholder) =>
  `<option value="">${escapeHtml(placeholder)}</option>${items.map((i) => `<option value="${i.id}">${escapeHtml(i.name)}</option>`).join('')}`;

// Reglas del CreateIncidentRequestDTO, validadas también aquí para avisar antes de enviar.
const RULES = {
  title: (v) => (!v ? 'El título es obligatorio.' : v.length < 5 || v.length > 150 ? 'Debe tener entre 5 y 150 caracteres.' : ''),
  description: (v) => (!v ? 'La descripción es obligatoria.' : v.length < 20 ? 'Describe el problema con al menos 20 caracteres.' : v.length > 4000 ? 'Máximo 4000 caracteres.' : ''),
  categoryId: (v) => (v ? '' : 'Elige una categoría.'),
  priority: (v) => (v ? '' : 'Elige una prioridad.'),
  userId: (v) => (v ? '' : 'Elige el usuario responsable.'),
};

export async function render(container, { route, signal }) {
  container.innerHTML = `
    <div class="page-header-row">
      ${pageHeader(route)}
      <button class="btn btn--primary" type="button" data-action="create" disabled>${icon('i-plus')}Nuevo incidente</button>
    </div>

    <div class="stack stack--tight">
      <form class="card filters" id="inc-filters" aria-label="Filtros de incidentes">
        <div class="filters__grid">
          <div class="field"><label for="f-category">Categoría</label><select class="input" id="f-category" name="categoryId"></select></div>
          <div class="field"><label for="f-priority">Prioridad</label>
            <select class="input" id="f-priority" name="priority"><option value="">Todas</option>${optionsHtml(PRIORIDAD)}</select></div>
          <div class="field"><label for="f-status">Estado</label>
            <select class="input" id="f-status" name="status"><option value="">Todos</option>${optionsHtml(ESTADO_INCIDENTE)}</select></div>
          <div class="field"><label for="f-user">Usuario responsable</label><select class="input" id="f-user" name="userId"></select></div>
          <div class="field"><label for="f-start">Desde</label><input class="input" id="f-start" name="startDate" type="date"></div>
          <div class="field"><label for="f-end">Hasta</label><input class="input" id="f-end" name="endDate" type="date"></div>
        </div>
        <p class="field__error" id="f-error" aria-live="polite"></p>
        <div class="form__actions">
          <button class="btn btn--primary" type="submit">${icon('i-filter')}Aplicar filtros</button>
          <button class="btn btn--ghost" type="reset">Limpiar</button>
        </div>
      </form>

      <section class="card" id="inc-table-card" aria-label="Listado de incidentes"><div id="inc-table"></div></section>
    </div>`;

  const form = container.querySelector('#inc-filters');
  const filterError = container.querySelector('#f-error');
  const createButton = container.querySelector('[data-action="create"]');
  let catalogs = { categories: [], activeCategories: [], users: [] };
  let incidents = [];

  const table = createDataTable(container.querySelector('#inc-table'), {
    caption: 'Incidentes registrados',
    searchPlaceholder: 'Buscar por título, categoría, usuario…',
    emptyMessage: 'No hay incidentes con estos filtros.',
    pageSize: 10,
    initialSort: { key: 'createdAt', dir: 'desc' },
    columns: [
      { key: 'id', label: 'ID', align: 'right', render: (r) => `<span class="cell-id">#${r.id}</span>`, text: (r) => String(r.id) },
      // Título y categoría en la misma celda: ahorra una columna y mantiene la tabla dentro de la pantalla
      {
        key: 'title', label: 'Incidente · categoría',
        render: (r) => `<span class="cell-stack"><span class="cell-title" title="${escapeHtml(r.title)}">${escapeHtml(r.title)}</span>
          <span class="cell-sub">${escapeHtml(r.categoryName)}</span></span>`,
        text: (r) => `${r.title} ${r.categoryName}`,
      },
      {
        key: 'priority', label: 'Prioridad', render: (r) => badge(PRIORIDAD, r.priority),
        text: (r) => labelOf(PRIORIDAD, r.priority), sortValue: (r) => Object.keys(PRIORIDAD).indexOf(r.priority),
      },
      {
        key: 'status', label: 'Estado', render: (r) => badge(ESTADO_INCIDENTE, r.status),
        text: (r) => labelOf(ESTADO_INCIDENTE, r.status), sortValue: (r) => Object.keys(ESTADO_INCIDENTE).indexOf(r.status),
      },
      { key: 'userFullName', label: 'Responsable', render: (r) => `<span class="person cell-muted">${avatar(r.userFullName)}${escapeHtml(r.userFullName)}</span>` },
      {
        key: 'createdAt', label: 'Fecha', render: (r) => `<span class="cell-muted" title="${formatDateTime(r.createdAt)}">${formatDate(r.createdAt)}</span>`,
        text: (r) => formatDateTime(r.createdAt), sortValue: (r) => parseDate(r.createdAt),
      },
      {
        key: 'actions', label: 'Acciones', sortable: false, searchable: false, text: () => '',
        render: (r) => `
          <span class="row-actions">
            <button class="icon-btn icon-btn--sm" type="button" data-row-action="view" data-id="${r.id}" aria-label="Ver detalle del incidente #${r.id}" title="Ver detalle">${icon('i-eye')}</button>
            <button class="icon-btn icon-btn--sm" type="button" data-row-action="ai" data-id="${r.id}" aria-label="Consultar a la IA sobre el incidente #${r.id}" title="Consultar a la IA" ${r.status === 'CLOSED' ? 'disabled' : ''}>${icon('i-sparkles')}</button>
            <button class="icon-btn icon-btn--sm" type="button" data-row-action="history" data-id="${r.id}" aria-label="Historial de IA del incidente #${r.id}" title="Historial de IA">${icon('i-history')}</button>
          </span>`,
      },
    ],
    onRowClick: (row) => openDetail(row),
  });

  // Botones de la columna Acciones (la tabla ignora los clics en botones para la fila).
  container.querySelector('#inc-table').addEventListener('click', (event) => {
    const button = event.target.closest('[data-row-action]');
    if (!button) return;
    const id = button.dataset.id;
    if (button.dataset.rowAction === 'ai') location.hash = `#/asistente?incidente=${id}`;
    else if (button.dataset.rowAction === 'history') location.hash = `#/historial?incidente=${id}`;
    else {
      const incident = incidents.find((i) => String(i.id) === id);
      if (incident) openDetail(incident);
    }
  });

  const readFilters = () => Object.fromEntries(new FormData(form));

  // Tras crear o actualizar, se respeta el filtro de estado ya aplicado.
  const showRows = () => {
    const { status } = readFilters();
    table.setRows(status ? incidents.filter((i) => i.status === status) : incidents);
  };

  const fill = () => {
    const keep = readFilters();
    form.elements.categoryId.innerHTML = selectOptions(catalogs.categories, 'Todas');
    form.elements.userId.innerHTML = selectOptions(catalogs.users, 'Todos');
    form.elements.categoryId.value = keep.categoryId ?? '';
    form.elements.userId.value = keep.userId ?? '';
  };

  async function load() {
    const filters = readFilters();
    if (filters.startDate && filters.endDate && filters.startDate > filters.endDate) {
      filterError.textContent = 'La fecha inicial no puede ser posterior a la final.';
      return;
    }
    filterError.textContent = '';
    table.setLoading(true);
    try {
      incidents = await listIncidents(filters, { signal });
    } catch (error) {
      if (signal.aborted) return;
      errorState(container.querySelector('#inc-table-card'), error, () => render(container, { route, signal }));
      return;
    }
    if (signal.aborted) return;
    table.setLoading(false);
    showRows();
  }

  form.addEventListener('submit', (event) => {
    event.preventDefault();
    load();
  });
  form.addEventListener('reset', () => setTimeout(load));
  createButton.addEventListener('click', () => openCreate());

  // Primera carga: incidentes y catálogos en paralelo.
  table.setLoading(true);
  try {
    const [rows, categories, users] = await Promise.all([
      listIncidents({}, { signal }),
      listCategories({}, { signal }),
      listUsers({}, { signal }),
    ]);
    incidents = rows;
    catalogs = {
      categories,
      activeCategories: categories.filter((c) => c.active),
      users: toOptions(users),
    };
  } catch (error) {
    if (signal.aborted) return;
    errorState(container.querySelector('#inc-table-card'), error, () => render(container, { route, signal }));
    return;
  }
  if (signal.aborted) return;
  fill();
  createButton.disabled = false;
  table.setLoading(false);
  showRows();

  // #/incidentes?id=12 abre directamente el detalle (enlaces del dashboard).
  // #/incidentes?nuevo=1 abre el formulario de registro (acceso rápido del dashboard).
  if (routeParams().get('nuevo') === '1') openCreate();

  const linkedId = routeParams().get('id');
  if (linkedId) {
    getIncident(linkedId, { signal })
      .then((incident) => { if (!signal.aborted) openDetail(incident); })
      .catch((error) => { if (!signal.aborted) toast(error.message, { type: 'error' }); });
  }

  /* ---------- Detalle y cambio de estado ---------- */
  function openDetail(incident) {
    const closed = incident.status === 'CLOSED';
    const modal = openModal({
      title: `Incidente #${incident.id}`,
      size: 'lg',
      body: `
        <h3 class="detail-title">${escapeHtml(incident.title)}</h3>
        <div class="cluster">${badge(PRIORIDAD, incident.priority)}${badge(ESTADO_INCIDENTE, incident.status)}</div>
        <dl class="kv kv--detail">
          <dt>Categoría</dt><dd>${escapeHtml(incident.categoryName)}</dd>
          <dt>Usuario responsable</dt><dd>${escapeHtml(incident.userFullName)}</dd>
          <dt>Registro</dt><dd>${formatDateTime(incident.createdAt)}</dd>
          <dt>Última actualización</dt><dd>${formatDateTime(incident.updatedAt)}</dd>
        </dl>
        <h4 class="detail-label">Descripción</h4>
        <p class="detail-text">${escapeHtml(incident.description)}</p>

        <form class="status-form" data-status-form>
          <div class="field">
            <label for="detail-status">Cambiar estado</label>
            <select class="input" id="detail-status" name="status" ${closed ? 'disabled' : ''}>${optionsHtml(ESTADO_INCIDENTE, incident.status)}</select>
            <p class="field__hint">${closed ? 'Un incidente cerrado ya no puede cambiar de estado.' : 'El cambio queda registrado en el backend.'}</p>
          </div>
          <button class="btn btn--primary" type="submit" ${closed ? 'disabled' : ''}>${icon('i-check')}Guardar estado</button>
        </form>

        <div class="detail-links">
          ${closed ? '' : `<a class="btn btn--ghost" href="#/asistente?incidente=${incident.id}">${icon('i-sparkles')}Consultar a la IA</a>`}
          <a class="btn btn--ghost" href="#/historial?incidente=${incident.id}">${icon('i-history')}Ver historial de IA</a>
        </div>`,
    });

    // Al seguir un enlace se cierra el modal (el router cambia de vista).
    modal.body.querySelectorAll('.detail-links a').forEach((a) => a.addEventListener('click', () => modal.close()));

    const statusForm = modal.body.querySelector('[data-status-form]');
    statusForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      const next = statusForm.elements.status.value;
      if (next === incident.status) {
        toast('El incidente ya tiene ese estado.', { type: 'info' });
        return;
      }
      // Cerrar es irreversible: el backend ya no permite cambiar el estado.
      if (next === 'CLOSED') {
        const ok = await confirmDialog({
          title: `Cerrar incidente #${incident.id}`,
          message: 'Un incidente cerrado ya no puede cambiar de estado ni consultarse con la IA. ¿Quieres cerrarlo?',
          confirmLabel: 'Cerrar incidente',
          danger: true,
        });
        if (!ok) return;
      }
      const button = statusForm.querySelector('button');
      setBusy(button, true, 'Guardando…');
      try {
        const updated = await updateIncidentStatus(incident.id, next);
        incidents = incidents.map((i) => (i.id === updated.id ? updated : i));
        showRows();
        toast(`Incidente #${updated.id}: ${labelOf(ESTADO_INCIDENTE, updated.status)}.`, { type: 'success' });
        modal.close();
      } catch (error) {
        setBusy(button, false);
        toast(error.message, { type: 'error' });
      }
    });
  }

  /* ---------- Registro ---------- */
  function openCreate() {
    const modal = openModal({
      title: 'Nuevo incidente',
      size: 'lg',
      body: `
        <form class="form" id="create-form" novalidate>
          <div class="field">
            <label for="c-title">Título</label>
            <input class="input" id="c-title" name="title" maxlength="150" autocomplete="off" aria-describedby="c-title-error" placeholder="Ej.: La VPN se desconecta cada 10 minutos">
            <p class="field__error" id="c-title-error" aria-live="polite"></p>
          </div>
          <div class="field">
            <label for="c-description">Descripción</label>
            <textarea class="input input--area" id="c-description" name="description" rows="5" maxlength="4000" aria-describedby="c-description-hint c-description-error"
              placeholder="Síntomas, mensajes de error, equipos afectados y desde cuándo ocurre"></textarea>
            <p class="field__hint" id="c-description-hint">Entre 20 y 4000 caracteres (<span data-counter>0/4000</span>). Mientras más detalle, mejor será la orientación del asistente IA.</p>
            <p class="field__error" id="c-description-error" aria-live="polite"></p>
          </div>
          <div class="form-row">
            <div class="field">
              <label for="c-category">Categoría</label>
              <select class="input" id="c-category" name="categoryId" aria-describedby="c-categoryId-error">${selectOptions(catalogs.activeCategories, 'Selecciona…')}</select>
              <p class="field__error" id="c-categoryId-error" aria-live="polite"></p>
            </div>
            <div class="field">
              <label for="c-priority">Prioridad</label>
              <select class="input" id="c-priority" name="priority" aria-describedby="c-priority-error"><option value="">Selecciona…</option>${optionsHtml(PRIORIDAD)}</select>
              <p class="field__error" id="c-priority-error" aria-live="polite"></p>
            </div>
            <div class="field">
              <label for="c-user">Usuario responsable</label>
              <select class="input" id="c-user" name="userId" aria-describedby="c-userId-error">${selectOptions(catalogs.users, 'Selecciona…')}</select>
              <p class="field__error" id="c-userId-error" aria-live="polite"></p>
            </div>
          </div>
          <div class="form__actions">
            <button class="btn btn--primary" type="submit">${icon('i-check')}Registrar incidente</button>
            <button class="btn btn--ghost" type="button" data-cancel>Cancelar</button>
          </div>
        </form>`,
    });

    const createForm = modal.body.querySelector('#create-form');
    const touched = new Set();
    attachCounter(createForm.elements.description, createForm.querySelector('[data-counter]'));

    const validateField = (name) => {
      const field = createForm.elements[name];
      const message = RULES[name](field.value.trim());
      createForm.querySelector(`#c-${name}-error`).textContent = message;
      field.setAttribute('aria-invalid', String(Boolean(message)));
      return !message;
    };

    Object.keys(RULES).forEach((name) => {
      const field = createForm.elements[name];
      field.addEventListener('blur', () => { touched.add(name); validateField(name); });
      field.addEventListener('input', () => { if (touched.has(name)) validateField(name); });
      field.addEventListener('change', () => { if (touched.has(name)) validateField(name); });
    });

    // Confirmar antes de descartar lo escrito.
    modal.body.querySelector('[data-cancel]').addEventListener('click', async () => {
      const dirty = [...new FormData(createForm).values()].some((v) => String(v).trim());
      if (dirty && !(await confirmDialog({ title: 'Descartar incidente', message: 'Se perderá lo que escribiste en el formulario.', confirmLabel: 'Descartar', danger: true }))) return;
      modal.close();
    });

    createForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      const results = Object.keys(RULES).map((name) => { touched.add(name); return validateField(name); });
      if (results.includes(false)) {
        createForm.querySelector('[aria-invalid="true"]')?.focus();
        return;
      }
      const button = createForm.querySelector('[type="submit"]');
      setBusy(button, true, 'Registrando…');
      try {
        const data = Object.fromEntries(new FormData(createForm));
        data.title = data.title.trim();
        data.description = data.description.trim();
        const created = await createIncident(data);
        incidents = [created, ...incidents];
        showRows();
        modal.close();
        toast(`Incidente #${created.id} registrado.`, { type: 'success' });
        openDetail(created);
      } catch (error) {
        setBusy(button, false);
        // 400 con errores por campo: se muestran junto a cada uno; el resto, en un aviso
        const shown = showServerFieldErrors(createForm, error, (name) => `c-${name}-error`);
        toast(shown ? 'Revisa los campos marcados.' : error.message, { type: 'error' });
      }
    });
  }
}
