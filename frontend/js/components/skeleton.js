// Loading placeholders. Shown while the backend response arrives;
// they keep the final shape to avoid content jumps.

// Wraps a placeholder as a "loading" region for screen readers
const loading = (html) => `<div role="status" aria-busy="true"><span class="sr-only">Cargando…</span>${html}</div>`;

// Placeholder for the KPI cards
export const skeletonKpis = (count = 5) => loading(`
  <div class="kpi-grid" aria-hidden="true">
    ${Array.from({ length: count }, () => `
      <div class="kpi kpi--skeleton">
        <span class="skeleton skeleton--text" style="width: 55%"></span>
        <span class="skeleton skeleton--title"></span>
        <span class="skeleton skeleton--text" style="width: 40%"></span>
      </div>`).join('')}
  </div>`);

// Placeholder for a chart
export const skeletonChart = (height = 220) => loading(`
  <div class="skeleton skeleton--chart" style="height: ${height}px" aria-hidden="true"></div>`);

// Placeholder for text lines
export const skeletonLines = (count = 3) => loading(`
  <div class="skeleton-lines" aria-hidden="true">
    ${Array.from({ length: count }, (_, i) => `
      <span class="skeleton skeleton--text" style="width: ${[92, 78, 64, 85][i % 4]}%"></span>`).join('')}
  </div>`);
