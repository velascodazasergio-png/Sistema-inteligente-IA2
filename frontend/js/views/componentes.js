// Catálogo de componentes (Etapa 2).
// ⚠ DATOS DE DEMOSTRACIÓN: todo lo que se muestra aquí es ficticio y solo
// sirve para revisar el diseño. Ningún módulo real importa estos datos.

import { pageHeader, icon, toast, escapeHtml } from '../ui.js';
import { kpiCard, animateCounters } from '../components/kpi-card.js';
import { barChart, columnChart, lineChart, donutChart } from '../components/charts.js';
import { createDataTable } from '../components/data-table.js';
import { openModal, confirmDialog } from '../components/modal.js';
import { createStarRating, starsHtml } from '../components/rating.js';
import { skeletonKpis, skeletonChart, skeletonLines } from '../components/skeleton.js';
import { ESTADO_INCIDENTE, PRIORIDAD, ESTADO_CONSULTA, badge } from '../catalogs.js';

const DEMO_CATEGORIAS = ['Redes y conectividad', 'Software', 'Hardware', 'Correo y colaboración', 'Accesos y cuentas', 'Impresión'];
const DEMO_TITULOS = [
  'Sin conexión a internet en piso 3',
  'Error al ejecutar el ERP',
  'Portátil no enciende',
  'Outlook no sincroniza',
  'Usuario bloqueado en el dominio',
  'Impresora no responde',
  'VPN se desconecta cada 10 minutos',
  'Pantalla azul al iniciar Windows',
];
const DEMO_RESPONSABLES = ['Ana Ruiz', 'Carlos Díaz', 'Laura Gómez', 'Andrés Pérez'];

function demoIncidentes() {
  const estados = Object.keys(ESTADO_INCIDENTE);
  const prioridades = Object.keys(PRIORIDAD);
  return Array.from({ length: 23 }, (_, i) => ({
    id: 1001 + i,
    titulo: DEMO_TITULOS[i % DEMO_TITULOS.length],
    categoria: DEMO_CATEGORIAS[i % DEMO_CATEGORIAS.length],
    prioridad: prioridades[(i * 3) % prioridades.length],
    estado: estados[(i * 2) % estados.length],
    fecha: new Date(2026, 9, 1 + ((i * 5) % 30), 8 + (i % 9)),
    responsable: DEMO_RESPONSABLES[i % DEMO_RESPONSABLES.length],
  }));
}

const dateFormat = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' });
const randomSeries = (n, base) => Array.from({ length: n }, () => Math.round(base * (0.5 + Math.random())));

const chartCard = (id, title, subtitle) => `
  <section class="card chart-card" aria-labelledby="${id}-title">
    <div class="chart-card__head">
      <div>
        <h3 class="card__title" id="${id}-title">${escapeHtml(title)}</h3>
        <p class="card__subtitle">${escapeHtml(subtitle)}</p>
      </div>
      ${id === 'demo-line' ? `<button class="btn btn--ghost" type="button" data-action="refresh-line">${icon('i-refresh')}Actualizar</button>` : ''}
    </div>
    <div id="${id}"></div>
  </section>`;

export function render(container, { signal }) {
  container.innerHTML = `
    ${pageHeader({
      eyebrow: 'Etapa 2',
      title: 'Catálogo de componentes',
      icon: 'i-layers',
      description: 'Piezas reutilizables para construir los módulos. Aquí se muestran con datos de demostración para revisar diseño e interacción.',
    })}

    <p class="note note--warning">${icon('i-alert')}<span><strong>Datos de demostración.</strong>
      Todas las cifras de esta página son ficticias. Los módulos reales tomarán sus datos de la API de Spring Boot.</span></p>

    <div class="stack" style="margin-top: var(--space-6)">
      <section aria-labelledby="sec-kpi">
        <h2 class="section-title" id="sec-kpi">Tarjetas KPI</h2>
        <div class="kpi-grid" id="demo-kpis">
          ${kpiCard({ label: 'Total de incidentes', value: 1284, icon: 'i-inbox', trend: { delta: 8.2, goodWhen: 'down', period: 'mes anterior' } })}
          ${kpiCard({ label: 'Pendientes', value: 96, icon: 'i-hourglass', tone: 'warning', trend: { delta: -12.5, goodWhen: 'down', period: 'mes anterior' } })}
          ${kpiCard({ label: 'Resueltos', value: 1188, icon: 'i-check-circle', tone: 'success', trend: { delta: 4.1, goodWhen: 'up', period: 'mes anterior' } })}
          ${kpiCard({ label: 'Porcentaje de resolución', value: 92.5, format: 'percent', icon: 'i-percent', tone: 'accent', progress: 92.5 })}
          ${kpiCard({ label: 'Tiempo promedio de atención', value: 14.6, format: 'duration', icon: 'i-clock', trend: { delta: 0, period: 'mes anterior' } })}
          ${kpiCard({ label: 'Indicador sin datos', value: null, icon: 'i-chart', hint: 'Así se ve si el backend no envía valor' })}
        </div>
      </section>

      <section aria-labelledby="sec-charts">
        <h2 class="section-title" id="sec-charts">Gráficos SVG</h2>
        <div class="grid-2">
          ${chartCard('demo-bar', 'Incidentes por categoría', 'Barras horizontales: nombres largos')}
          ${chartCard('demo-donut', 'Incidentes por estado', 'Dona: proporciones')}
          ${chartCard('demo-line', 'Evolución semanal', 'Líneas: varias series en el tiempo')}
          ${chartCard('demo-columns', 'Tiempos de resolución', 'Columnas: distribución en rangos de horas')}
          ${chartCard('demo-empty', 'Estado vacío', 'Cuando los filtros no devuelven datos')}
          ${chartCard('demo-priority', 'Incidentes por prioridad', 'Dona con colores semánticos')}
        </div>
      </section>

      <section aria-labelledby="sec-table">
        <h2 class="section-title" id="sec-table">Tabla de datos</h2>
        <div class="card"><div id="demo-table"></div></div>
      </section>

      <section aria-labelledby="sec-feedback">
        <h2 class="section-title" id="sec-feedback">Modales y notificaciones</h2>
        <div class="card cluster">
          <button class="btn btn--primary" type="button" data-action="modal">${icon('i-eye')}Abrir detalle</button>
          <button class="btn btn--ghost" type="button" data-action="confirm">Confirmar acción</button>
          <button class="btn btn--ghost" type="button" data-action="toast-success">Toast éxito</button>
          <button class="btn btn--ghost" type="button" data-action="toast-warning">Toast advertencia</button>
          <button class="btn btn--ghost" type="button" data-action="toast-error">Toast error</button>
        </div>
      </section>

      <div class="grid-2">
        <section class="card" aria-labelledby="sec-rating">
          <h2 class="card__title" id="sec-rating">Calificación</h2>
          <p class="card__subtitle">Interactiva con mouse y teclado (flechas). Abajo, versión de solo lectura.</p>
          <div id="demo-rating"></div>
          <p class="field__hint" id="demo-rating-value" aria-live="polite">Sin calificar</p>
          <div style="margin-top: var(--space-4)">${starsHtml(4)}</div>
        </section>

        <section class="card" aria-labelledby="sec-badges">
          <h2 class="card__title" id="sec-badges">Etiquetas de estado</h2>
          <p class="card__subtitle">Valores de los enums del backend.</p>
          <div class="cluster">${Object.keys(ESTADO_INCIDENTE).map((k) => badge(ESTADO_INCIDENTE, k)).join('')}</div>
          <div class="cluster" style="margin-top: var(--space-3)">${Object.keys(PRIORIDAD).map((k) => badge(PRIORIDAD, k)).join('')}</div>
          <div class="cluster" style="margin-top: var(--space-3)">${Object.keys(ESTADO_CONSULTA).map((k) => badge(ESTADO_CONSULTA, k)).join('')}</div>
        </section>
      </div>

      <section aria-labelledby="sec-skeleton">
        <h2 class="section-title" id="sec-skeleton">Skeleton loaders</h2>
        <div class="stack" style="gap: var(--space-4)">
          ${skeletonKpis(4)}
          <div class="grid-2">
            <div class="card">${skeletonChart(180)}</div>
            <div class="card">${skeletonLines(4)}</div>
          </div>
        </div>
      </section>
    </div>`;

  animateCounters(container.querySelector('#demo-kpis'));

  // Gráficos
  const counts = [412, 318, 205, 164, 121, 64];
  barChart(container.querySelector('#demo-bar'), {
    title: 'Incidentes por categoría (demostración)',
    data: DEMO_CATEGORIAS.map((label, i) => ({ label, value: counts[i] })),
    signal,
  });

  donutChart(container.querySelector('#demo-donut'), {
    title: 'Incidentes por estado (demostración)',
    data: Object.entries(ESTADO_INCIDENTE).map(([, e], i) => ({ label: e.label, value: [64, 21, 32, 980, 187][i], color: e.color })),
    signal,
  });

  const weeks = Array.from({ length: 12 }, (_, i) => `Sem ${i + 1}`);
  const line = lineChart(container.querySelector('#demo-line'), {
    title: 'Evolución semanal (demostración)',
    data: {
      labels: weeks,
      series: [
        { name: 'Registrados', values: [92, 104, 88, 120, 131, 98, 110, 142, 125, 117, 136, 128] },
        { name: 'Resueltos', values: [80, 97, 90, 105, 118, 101, 99, 130, 128, 112, 129, 131] },
      ],
    },
    signal,
  });

  columnChart(container.querySelector('#demo-columns'), {
    title: 'Distribución de tiempos de resolución (demostración)',
    labelHeader: 'Rango',
    data: [
      { label: '< 1 h', value: 186 },
      { label: '1–4 h', value: 342 },
      { label: '4–8 h', value: 298 },
      { label: '8–24 h', value: 214 },
      { label: '1–3 d', value: 112 },
      { label: '> 3 d', value: 36 },
    ],
    signal,
  });

  columnChart(container.querySelector('#demo-empty'), { title: 'Sin datos', data: [], signal });

  donutChart(container.querySelector('#demo-priority'), {
    title: 'Incidentes por prioridad (demostración)',
    data: Object.values(PRIORIDAD).map((p, i) => ({ label: p.label, value: [402, 511, 287, 84][i], color: p.color })),
    signal,
  });

  // Tabla
  const table = createDataTable(container.querySelector('#demo-table'), {
    caption: 'Incidentes de demostración',
    searchPlaceholder: 'Buscar por título, categoría, responsable…',
    pageSize: 8,
    initialSort: { key: 'fecha', dir: 'desc' },
    columns: [
      { key: 'id', label: 'ID', align: 'right', render: (r) => `<span class="cell-id">#${r.id}</span>` },
      { key: 'titulo', label: 'Título', render: (r) => `<span class="cell-title" title="${escapeHtml(r.titulo)}">${escapeHtml(r.titulo)}</span>` },
      { key: 'categoria', label: 'Categoría', render: (r) => `<span class="cell-muted">${escapeHtml(r.categoria)}</span>` },
      {
        key: 'prioridad', label: 'Prioridad', render: (r) => badge(PRIORIDAD, r.prioridad),
        text: (r) => PRIORIDAD[r.prioridad]?.label, sortValue: (r) => Object.keys(PRIORIDAD).indexOf(r.prioridad),
      },
      { key: 'estado', label: 'Estado', render: (r) => badge(ESTADO_INCIDENTE, r.estado), text: (r) => ESTADO_INCIDENTE[r.estado]?.label },
      { key: 'fecha', label: 'Fecha', render: (r) => `<span class="cell-muted">${dateFormat.format(r.fecha)}</span>`, text: (r) => dateFormat.format(r.fecha) },
      { key: 'responsable', label: 'Responsable' },
      {
        key: 'acciones', label: 'Acciones', sortable: false, searchable: false,
        render: (r) => `<button class="icon-btn icon-btn--sm" type="button" data-view="${r.id}" aria-label="Ver incidente ${r.id}">${icon('i-eye')}</button>`,
      },
    ],
    onRowClick: (row) => openDetail(row),
  });
  table.setLoading(true);
  setTimeout(() => !signal.aborted && table.setRows(demoIncidentes()), 700);

  // Calificación
  const valueEl = container.querySelector('#demo-rating-value');
  createStarRating(container.querySelector('#demo-rating'), {
    label: 'Utilidad de la respuesta',
    onChange: (value) => {
      valueEl.textContent = `Calificación: ${value} de 5`;
    },
  });

  // Acciones de la página
  const incidentes = demoIncidentes();
  function openDetail(row) {
    openModal({
      title: `Incidente #${row.id}`,
      body: `
        <dl class="kv">
          <dt>Título</dt><dd>${escapeHtml(row.titulo)}</dd>
          <dt>Categoría</dt><dd>${escapeHtml(row.categoria)}</dd>
          <dt>Prioridad</dt><dd>${badge(PRIORIDAD, row.prioridad)}</dd>
          <dt>Estado</dt><dd>${badge(ESTADO_INCIDENTE, row.estado)}</dd>
          <dt>Responsable</dt><dd>${escapeHtml(row.responsable)}</dd>
          <dt>Fecha</dt><dd>${dateFormat.format(row.fecha)}</dd>
        </dl>
        <p class="field__hint">Datos de demostración.</p>`,
      actions: [{ label: 'Cerrar', variant: 'primary', autofocus: true }],
    });
  }

  container.addEventListener('click', async (event) => {
    const view = event.target.closest('[data-view]');
    if (view) {
      openDetail(incidentes.find((r) => r.id === Number(view.dataset.view)));
      return;
    }
    const action = event.target.closest('[data-action]')?.dataset.action;
    if (action === 'modal') openDetail(incidentes[0]);
    if (action === 'confirm') {
      const ok = await confirmDialog({
        title: 'Desactivar artículo',
        message: 'El artículo dejará de usarse como fuente del asistente. ¿Quieres continuar?',
        confirmLabel: 'Desactivar',
        danger: true,
      });
      toast(ok ? 'Acción confirmada (demostración).' : 'Acción cancelada.', { type: ok ? 'success' : 'info' });
    }
    if (action === 'toast-success') toast('Incidente registrado correctamente.', { type: 'success' });
    if (action === 'toast-warning') toast('No se encontraron fuentes suficientemente relevantes.', { type: 'warning' });
    if (action === 'toast-error') toast('El servicio de embeddings no respondió. Intenta de nuevo.', { type: 'error' });
    if (action === 'refresh-line') {
      line.update({
        labels: weeks,
        series: [
          { name: 'Registrados', values: randomSeries(12, 115) },
          { name: 'Resueltos', values: randomSeries(12, 108) },
        ],
      });
    }
  }, { signal });
}
