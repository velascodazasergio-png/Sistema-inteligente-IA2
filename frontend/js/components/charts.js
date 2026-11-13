// Gráficos SVG nativos, sin librerías: barras horizontales, columnas,
// líneas y dona. Todos comparten:
//  - tooltip al pasar el cursor
//  - animación de entrada (se desactiva con prefers-reduced-motion)
//  - estado vacío cuando no hay datos
//  - redibujado al cambiar el tamaño del contenedor
//  - tabla oculta con los mismos datos para lectores de pantalla
//
// Cada función devuelve { update(data), destroy() }. Pasa `signal` (el que
// el router entrega a cada vista) para liberar recursos al cambiar de sección.

import { escapeHtml, icon } from '../ui.js';

const SVG_NS = 'http://www.w3.org/2000/svg';
export const CHART_COLORS = ['--chart-1', '--chart-2', '--chart-3', '--chart-4', '--chart-5', '--chart-6']
  .map((name) => `var(${name})`);

const numberFormat = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 });
export const formatNumber = (value) => numberFormat.format(value);

let uid = 0;

/* ---------- Utilidades ---------- */
function svgEl(name, attrs = {}, parent) {
  const node = document.createElementNS(SVG_NS, name);
  for (const [key, value] of Object.entries(attrs)) {
    if (value !== undefined && value !== null) node.setAttribute(key, value);
  }
  parent?.append(node);
  return node;
}

function svgText(parent, text, attrs) {
  const node = svgEl('text', attrs, parent);
  node.textContent = text;
  return node;
}

function truncate(text, maxChars) {
  const value = String(text);
  return value.length > maxChars ? `${value.slice(0, Math.max(1, maxChars - 1))}…` : value;
}

const percent = (value, total) => (total ? `${formatNumber((value / total) * 100)} %` : '—');

// Escala "redonda" para el eje Y: 0, 5, 10, 15, 20…
function niceScale(maxValue, ticks = 4, integer = true) {
  if (!(maxValue > 0)) return { max: ticks, step: 1 };
  let step = maxValue / ticks;
  const magnitude = 10 ** Math.floor(Math.log10(step));
  const normalized = step / magnitude;
  step = (normalized <= 1 ? 1 : normalized <= 2 ? 2 : normalized <= 5 ? 5 : 10) * magnitude;
  if (integer) step = Math.max(1, Math.ceil(step));
  return { max: step * ticks, step };
}

function gradient(defs, { vertical = false, from, to, fromOpacity = 1, toOpacity = 1 }) {
  const id = `chart-grad-${++uid}`;
  const grad = svgEl('linearGradient', { id, x1: 0, y1: vertical ? 1 : 0, x2: vertical ? 0 : 1, y2: 0 }, defs);
  const start = svgEl('stop', { offset: 0, 'stop-opacity': fromOpacity }, grad);
  const end = svgEl('stop', { offset: 1, 'stop-opacity': toOpacity }, grad);
  start.style.stopColor = from;
  end.style.stopColor = to;
  return `url(#${id})`;
}

function drawYAxis(svg, { m, iw, ih, max, step, format }) {
  for (let v = 0; v <= max + 1e-9; v += step) {
    const y = m.top + ih - (v / max) * ih;
    svgEl('line', { x1: m.left, x2: m.left + iw, y1: y, y2: y, class: v === 0 ? 'axis-line' : 'grid-line' }, svg);
    svgText(svg, format(v), { x: m.left - 8, y: y + 4, 'text-anchor': 'end' });
  }
}

// Cada cuántas etiquetas del eje X mostrar una, para que no se monten.
const labelEvery = (count, innerWidth, minSpace = 64) => Math.max(1, Math.ceil((count * minSpace) / innerWidth));

/* ---------- Tooltip compartido ---------- */
let tooltipEl;

function showTooltip(html, x, y) {
  if (!tooltipEl) {
    tooltipEl = document.createElement('div');
    tooltipEl.className = 'chart-tooltip';
    tooltipEl.setAttribute('aria-hidden', 'true');
    document.body.append(tooltipEl);
  }
  tooltipEl.innerHTML = html;
  tooltipEl.hidden = false;
  const { width, height } = tooltipEl.getBoundingClientRect();
  let left = x + 14;
  if (left + width > window.innerWidth - 8) left = x - width - 14;
  let top = y - height - 12;
  if (top < 8) top = y + 18;
  tooltipEl.style.translate = `${Math.max(8, left)}px ${top}px`;
}

function hideTooltip() {
  if (tooltipEl) tooltipEl.hidden = true;
}

function bindTooltip(node, html) {
  node.addEventListener('pointermove', (event) => showTooltip(html, event.clientX, event.clientY));
  node.addEventListener('pointerleave', hideTooltip);
}

const tooltipRow = (label, value, color) => `
  <span class="tt-row">
    ${color ? `<i class="swatch" style="--swatch:${escapeHtml(color)}"></i>` : ''}
    <span>${escapeHtml(label)}</span><b>${escapeHtml(value)}</b>
  </span>`;

const legendHtml = (items) => `
  <ul class="chart-legend">
    ${items.map((item) => `
      <li>
        <i class="swatch" style="--swatch:${escapeHtml(item.color)}"></i>
        <span>${escapeHtml(item.label)}</span>
        ${item.value !== undefined ? `<strong>${escapeHtml(item.value)}</strong>` : ''}
      </li>`).join('')}
  </ul>`;

const srTable = (title, headers, rows) => `
  <table class="sr-only">
    <caption>${escapeHtml(title)}</caption>
    <thead><tr>${headers.map((h) => `<th scope="col">${escapeHtml(h)}</th>`).join('')}</tr></thead>
    <tbody>${rows.map((row) => `<tr>${row.map((cell) => `<td>${escapeHtml(cell)}</td>`).join('')}</tr>`).join('')}</tbody>
  </table>`;

const emptyHtml = (message) => `
  <div class="chart-empty">
    ${icon('i-chart')}
    <span>${escapeHtml(message)}</span>
  </div>`;

/* ---------- Ciclo de vida común ---------- */
function createChart(container, options, { isEmpty, draw, table }) {
  const state = { format: formatNumber, emptyMessage: 'No hay datos para los filtros seleccionados.', ...options };
  let lastWidth = 0;
  let animate = true;

  container.classList.add('chart');

  const render = () => {
    const width = Math.round(container.clientWidth);
    if (!width) return;
    lastWidth = width;
    hideTooltip();

    if (isEmpty(state.data)) {
      container.innerHTML = emptyHtml(state.emptyMessage);
      return;
    }

    const node = draw(state, width);
    const svg = node instanceof SVGSVGElement ? node : node.querySelector('svg');
    svg.setAttribute('role', 'img');
    svg.setAttribute('aria-label', state.title ?? 'Gráfico');
    if (animate) svg.classList.add('chart--animate');
    animate = false;

    const { headers, rows } = table(state);
    container.replaceChildren(node);
    container.insertAdjacentHTML('beforeend', srTable(state.title ?? 'Datos del gráfico', headers, rows));
  };

  const observer = new ResizeObserver(([entry]) => {
    const width = Math.round(entry.contentRect.width);
    if (width && width !== lastWidth) render();
  });
  observer.observe(container);
  render();

  const destroy = () => {
    observer.disconnect();
    hideTooltip();
  };
  state.signal?.addEventListener('abort', destroy, { once: true });

  return {
    update(data) {
      state.data = data;
      animate = true;
      render();
    },
    destroy,
  };
}

/* ---------- Barras horizontales (categorías con nombres largos) ---------- */
// data: [{ label, value, color? }]
export function barChart(container, options) {
  return createChart(container, options, {
    isEmpty: (data) => !data?.length,
    table: ({ data, format, labelHeader = 'Categoría', valueHeader = 'Valor' }) => ({
      headers: [labelHeader, valueHeader],
      rows: data.map((d) => [d.label, format(d.value)]),
    }),
    draw({ data, format }, width) {
      const rowHeight = 46;
      const barHeight = 10;
      const height = data.length * rowHeight;
      const max = Math.max(...data.map((d) => d.value)) || 1;
      const total = data.reduce((sum, d) => sum + d.value, 0);
      const maxChars = Math.floor(width / 7.5) - 8;

      const svg = svgEl('svg', { width, height, viewBox: `0 0 ${width} ${height}` });
      const defs = svgEl('defs', {}, svg);
      const fill = gradient(defs, { from: 'var(--chart-1)', to: 'var(--chart-2)' });

      data.forEach((d, i) => {
        const y = i * rowHeight;
        const group = svgEl('g', { class: 'mark' }, svg);
        svgText(group, truncate(d.label, maxChars), { x: 0, y: y + 16, class: 'chart-label' });
        svgText(group, format(d.value), { x: width, y: y + 16, 'text-anchor': 'end', class: 'chart-value' });
        svgEl('rect', { x: 0, y: y + 24, width, height: barHeight, rx: barHeight / 2, class: 'bar-track' }, group);
        const barWidth = d.value > 0 ? Math.max((d.value / max) * width, barHeight) : 0;
        const bar = svgEl('rect', { x: 0, y: y + 24, width: barWidth, height: barHeight, rx: barHeight / 2, class: 'bar-h' }, group);
        bar.style.fill = d.color ?? fill;
        bar.style.animationDelay = `${i * 60}ms`;
        bindTooltip(group, `<strong>${escapeHtml(d.label)}</strong>${tooltipRow('Total', `${format(d.value)} · ${percent(d.value, total)}`)}`);
      });
      return svg;
    },
  });
}

/* ---------- Columnas verticales (series cortas, histogramas) ---------- */
// data: [{ label, value, color? }]
export function columnChart(container, options) {
  return createChart(container, options, {
    isEmpty: (data) => !data?.length,
    table: ({ data, format, labelHeader = 'Periodo', valueHeader = 'Valor' }) => ({
      headers: [labelHeader, valueHeader],
      rows: data.map((d) => [d.label, format(d.value)]),
    }),
    draw({ data, format, height = 240, integer = true }, width) {
      const { max, step } = niceScale(Math.max(...data.map((d) => d.value)), 4, integer);
      const m = { top: 12, right: 4, bottom: 28, left: 14 + format(max).length * 6.5 };
      const iw = width - m.left - m.right;
      const ih = height - m.top - m.bottom;

      const svg = svgEl('svg', { width, height, viewBox: `0 0 ${width} ${height}` });
      const defs = svgEl('defs', {}, svg);
      const fill = gradient(defs, { vertical: true, from: 'var(--chart-1)', to: 'var(--chart-2)' });
      drawYAxis(svg, { m, iw, ih, max, step, format });

      const band = iw / data.length;
      const barWidth = Math.min(band * 0.62, 44);
      const every = labelEvery(data.length, iw);

      data.forEach((d, i) => {
        const group = svgEl('g', { class: 'mark' }, svg);
        svgEl('rect', { x: m.left + band * i, y: m.top, width: band, height: ih, class: 'hit' }, group);
        const h = (d.value / max) * ih;
        const x = m.left + band * i + (band - barWidth) / 2;
        const bar = svgEl('rect', {
          x, y: m.top + ih - h, width: barWidth, height: h, rx: Math.min(4, barWidth / 4), class: 'bar-v',
        }, group);
        bar.style.fill = d.color ?? fill;
        bar.style.animationDelay = `${i * 40}ms`;
        if (i % every === 0) {
          svgText(group, truncate(d.label, Math.floor((band * every) / 7)), {
            x: x + barWidth / 2, y: height - 8, 'text-anchor': 'middle',
          });
        }
        bindTooltip(group, `<strong>${escapeHtml(d.label)}</strong>${tooltipRow('Valor', format(d.value))}`);
      });
      return svg;
    },
  });
}

/* ---------- Líneas (evolución en el tiempo, varias series) ---------- */
// data: { labels: ['Sem 1', …], series: [{ name, values: [n, …], color? }] }
export function lineChart(container, options) {
  return createChart(container, options, {
    isEmpty: (data) => !data?.labels?.length || !data.series?.length,
    table: ({ data, format, labelHeader = 'Periodo' }) => ({
      headers: [labelHeader, ...data.series.map((s) => s.name)],
      rows: data.labels.map((label, i) => [label, ...data.series.map((s) => format(s.values[i] ?? 0))]),
    }),
    draw({ data, format, height = 260, integer = true }, width) {
      const { labels, series } = data;
      const colors = series.map((s, i) => s.color ?? CHART_COLORS[i % CHART_COLORS.length]);
      const { max, step } = niceScale(Math.max(...series.flatMap((s) => s.values)), 4, integer);
      const m = { top: 16, right: 12, bottom: 28, left: 14 + format(max).length * 6.5 };
      const iw = width - m.left - m.right;
      const ih = height - m.top - m.bottom;
      const n = labels.length;
      const xAt = (i) => m.left + (n === 1 ? iw / 2 : (i / (n - 1)) * iw);
      const yAt = (v) => m.top + ih - (v / max) * ih;

      const wrapper = document.createElement('div');
      const svg = svgEl('svg', { width, height, viewBox: `0 0 ${width} ${height}` });
      wrapper.append(svg);
      const defs = svgEl('defs', {}, svg);
      drawYAxis(svg, { m, iw, ih, max, step, format });

      const every = labelEvery(n, iw);
      labels.forEach((label, i) => {
        if (i % every === 0) svgText(svg, truncate(label, 12), { x: xAt(i), y: height - 8, 'text-anchor': 'middle' });
      });

      series.forEach((s, si) => {
        const points = s.values.map((v, i) => [xAt(i), yAt(v ?? 0)]);
        const line = points.map(([x, y], i) => `${i ? 'L' : 'M'}${x.toFixed(1)},${y.toFixed(1)}`).join('');
        const baseline = m.top + ih;
        const area = svgEl('path', {
          d: `${line}L${points.at(-1)[0].toFixed(1)},${baseline}L${points[0][0].toFixed(1)},${baseline}Z`,
          class: 'line-area',
        }, svg);
        area.style.fill = gradient(defs, { vertical: true, from: colors[si], to: colors[si], fromOpacity: 0, toOpacity: 0.28 });
        const path = svgEl('path', { d: line, class: 'line-path', pathLength: 1 }, svg);
        path.style.stroke = colors[si];
        if (n <= 40) {
          points.forEach(([x, y]) => {
            const dot = svgEl('circle', { cx: x, cy: y, r: 3, class: 'line-dot' }, svg);
            dot.style.stroke = colors[si];
          });
        }
      });

      // Columnas invisibles para el tooltip con todas las series del periodo.
      const guide = svgEl('line', { y1: m.top, y2: m.top + ih, class: 'line-guide' }, svg);
      const slot = n > 1 ? iw / (n - 1) : iw;
      labels.forEach((label, i) => {
        const hit = svgEl('rect', { x: xAt(i) - slot / 2, y: m.top, width: slot, height: ih, class: 'hit' }, svg);
        hit.addEventListener('pointerenter', () => {
          guide.setAttribute('x1', xAt(i));
          guide.setAttribute('x2', xAt(i));
          guide.classList.add('is-visible');
        });
        hit.addEventListener('pointerleave', () => guide.classList.remove('is-visible'));
        bindTooltip(hit, `<strong>${escapeHtml(label)}</strong>${series
          .map((s, si) => tooltipRow(s.name, format(s.values[i] ?? 0), colors[si])).join('')}`);
      });

      wrapper.insertAdjacentHTML('beforeend', legendHtml(series.map((s, si) => ({ label: s.name, color: colors[si] }))));
      return wrapper;
    },
  });
}

/* ---------- Dona (proporciones: estados, prioridades) ---------- */
// data: [{ label, value, color? }]
export function donutChart(container, options) {
  return createChart(container, options, {
    isEmpty: (data) => !data?.length || data.every((d) => !d.value),
    table: ({ data, format, labelHeader = 'Categoría' }) => {
      const total = data.reduce((sum, d) => sum + d.value, 0);
      return {
        headers: [labelHeader, 'Valor', 'Porcentaje'],
        rows: data.map((d) => [d.label, format(d.value), percent(d.value, total)]),
      };
    },
    draw({ data, format, size = 200, centerLabel = 'Total' }, width) {
      const diameter = Math.min(size, width);
      const stroke = Math.max(16, diameter * 0.12);
      const r = (diameter - stroke) / 2;
      const c = 2 * Math.PI * r;
      const center = diameter / 2;
      const total = data.reduce((sum, d) => sum + d.value, 0);
      const colors = data.map((d, i) => d.color ?? CHART_COLORS[i % CHART_COLORS.length]);
      const gap = data.filter((d) => d.value).length > 1 ? 2 : 0;

      const wrapper = document.createElement('div');
      wrapper.className = 'donut-layout';
      const svg = svgEl('svg', { width: diameter, height: diameter, viewBox: `0 0 ${diameter} ${diameter}`, class: 'donut-svg' });
      wrapper.append(svg);

      // El grupo exterior se anima; el interior rota para empezar arriba.
      const ring = svgEl('g', { class: 'donut' }, svg);
      const rotated = svgEl('g', { transform: `rotate(-90 ${center} ${center})` }, ring);
      svgEl('circle', { cx: center, cy: center, r, 'stroke-width': stroke, class: 'donut-track' }, rotated);

      let offset = 0;
      data.forEach((d, i) => {
        if (!d.value) return;
        const length = (d.value / total) * c;
        const segment = svgEl('circle', {
          cx: center, cy: center, r, 'stroke-width': stroke, class: 'mark donut-seg',
          'stroke-dasharray': `${Math.max(length - gap, 0.5)} ${c}`,
          'stroke-dashoffset': -offset,
        }, rotated);
        segment.style.stroke = colors[i];
        offset += length;
        bindTooltip(segment, `<strong>${escapeHtml(d.label)}</strong>${tooltipRow('Total', `${format(d.value)} · ${percent(d.value, total)}`, colors[i])}`);
      });

      svgText(svg, format(total), { x: center, y: center + 4, 'text-anchor': 'middle', class: 'donut-total' });
      svgText(svg, centerLabel, { x: center, y: center + 24, 'text-anchor': 'middle', class: 'donut-caption' });

      wrapper.insertAdjacentHTML('beforeend', legendHtml(data.map((d, i) => ({
        label: d.label,
        color: colors[i],
        value: `${format(d.value)} · ${percent(d.value, total)}`,
      }))));
      return wrapper;
    },
  });
}
