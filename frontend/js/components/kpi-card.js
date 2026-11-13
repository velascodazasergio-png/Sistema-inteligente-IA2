// Tarjeta de indicador (KPI) con icono, valor, tendencia y barra de progreso.
// El valor siempre viene del backend; si no hay valor se muestra "—".

import { icon, escapeHtml } from '../ui.js';

const numberFormat = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 });

// Horas → texto legible: 45 min · 12,5 h · 3 d 4 h
export function formatDuration(hours) {
  if (!Number.isFinite(hours)) return '—';
  if (hours < 1) return `${Math.round(hours * 60)} min`;
  if (hours < 48) return `${numberFormat.format(hours)} h`;
  const days = Math.floor(hours / 24);
  const rest = Math.round(hours % 24);
  return rest ? `${days} d ${rest} h` : `${days} d`;
}

const FORMATTERS = {
  number: (v) => numberFormat.format(v),
  percent: (v) => `${numberFormat.format(v)} %`,
  duration: formatDuration,
};

function trendHtml(trend) {
  if (!trend || !Number.isFinite(trend.delta)) return '';
  const { delta, goodWhen = 'up', period = 'periodo anterior' } = trend;
  const direction = delta > 0 ? 'up' : delta < 0 ? 'down' : 'flat';
  const quality = direction === 'flat' ? 'flat' : direction === goodWhen ? 'good' : 'bad';
  const arrow = direction === 'flat' ? '' : icon(direction === 'up' ? 'i-trend-up' : 'i-trend-down');
  const sign = delta > 0 ? '+' : '';
  return `
    <span class="trend trend--${quality}">${arrow}${sign}${numberFormat.format(delta)} %</span>
    <span>vs. ${escapeHtml(period)}</span>`;
}

/**
 * @param {object} o
 * @param {string} o.label     Texto del indicador.
 * @param {number|null} o.value Valor real recibido del backend.
 * @param {'number'|'percent'|'duration'} [o.format]
 * @param {string} o.icon      Id del icono del sprite.
 * @param {'primary'|'accent'|'success'|'warning'|'danger'|'info'|'pink'} [o.tone]
 * @param {{delta:number, goodWhen?:'up'|'down', period?:string}} [o.trend]
 *        Variación porcentual frente al periodo anterior (si hay histórico).
 * @param {number} [o.progress] 0–100: dibuja una barra de progreso.
 * @param {string} [o.hint]    Texto auxiliar en el pie.
 * @param {number[]} [o.spark] Serie corta (p. ej. semanal) para un minigráfico de tendencia.
 */
export function kpiCard({ label, value, format = 'number', icon: iconName, tone = 'primary', trend, progress, hint, spark }) {
  const hasValue = typeof value === 'number' && Number.isFinite(value);
  const fmt = FORMATTERS[format] ?? FORMATTERS.number;
  const clamped = Math.min(100, Math.max(0, progress ?? 0));

  return `
    <article class="kpi" data-tone="${tone}">
      <div class="kpi__head">
        <h3 class="kpi__label">${escapeHtml(label)}</h3>
        <span class="kpi__icon">${icon(iconName)}</span>
      </div>
      <p class="kpi__value" ${hasValue ? `data-count-to="${value}" data-format="${format}"` : ''}>${hasValue ? fmt(value) : '—'}</p>
      ${progress != null && hasValue ? `
        <div class="kpi__progress" role="progressbar" aria-label="${escapeHtml(label)}"
             aria-valuemin="0" aria-valuemax="100" aria-valuenow="${clamped}">
          <span style="--progress:${clamped}%"></span>
        </div>` : ''}
      ${hasValue ? sparkHtml(spark, label) : ''}
      <div class="kpi__foot">
        ${hasValue ? trendHtml(trend) : '<span>Sin datos</span>'}
        ${hint ? `<span>${escapeHtml(hint)}</span>` : ''}
      </div>
    </article>`;
}

// Minigráfico de línea y área. Usa el color del tono de la tarjeta (--kpi-color).
function sparkHtml(values, label) {
  const data = (values ?? []).filter(Number.isFinite);
  if (data.length < 2) return '';
  const w = 120;
  const h = 32;
  const min = Math.min(...data);
  const max = Math.max(...data);
  const span = max - min || 1;
  const points = data.map((v, i) => [(i / (data.length - 1)) * w, h - 3 - ((v - min) / span) * (h - 6)]);
  const line = points.map(([x, y], i) => `${i ? 'L' : 'M'}${x.toFixed(1)} ${y.toFixed(1)}`).join(' ');
  return `
    <svg class="kpi__spark" viewBox="0 0 ${w} ${h}" preserveAspectRatio="none" role="img"
         aria-label="Tendencia de ${escapeHtml(label)}: ${data.join(', ')}">
      <path class="kpi__spark-area" d="${line} L${w} ${h} L0 ${h} Z"/>
      <path class="kpi__spark-line" d="${line}"/>
    </svg>`;
}

// Cuenta de 0 al valor real cuando la tarjeta entra en pantalla (una vez).
export function animateCounters(root) {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

  const run = (el) => {
    const target = Number(el.dataset.countTo);
    const fmt = FORMATTERS[el.dataset.format] ?? FORMATTERS.number;
    const integer = Number.isInteger(target) && el.dataset.format === 'number';
    const start = performance.now();
    const duration = 900;
    const tick = (now) => {
      const t = Math.min((now - start) / duration, 1);
      const current = target * (1 - (1 - t) ** 3);
      el.textContent = fmt(integer ? Math.round(current) : current);
      if (t < 1) requestAnimationFrame(tick);
      else el.textContent = fmt(target);
    };
    requestAnimationFrame(tick);
  };

  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      observer.unobserve(entry.target);
      run(entry.target);
    });
  }, { threshold: 0.4 });

  root.querySelectorAll('[data-count-to]').forEach((el) => observer.observe(el));
}
