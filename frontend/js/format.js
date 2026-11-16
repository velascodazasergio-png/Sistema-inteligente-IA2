// Date and number formatting for backend data.
// Spring sends LocalDateTime without a time zone ("2026-11-08T15:19:00"):
// it is read as local time, which is how it was recorded.

const dateFmt = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium' });
const dateTimeFmt = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
const percentFmt = new Intl.NumberFormat('es-CO', { style: 'percent', maximumFractionDigits: 0 });

// Backend date text → Date, or null if it is not valid
export const parseDate = (value) => {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
};

// Date in short Spanish format, or "—"
export const formatDate = (value) => {
  const date = parseDate(value);
  return date ? dateFmt.format(date) : '—';
};

// Date and time in Spanish format, or "—"
export const formatDateTime = (value) => {
  const date = parseDate(value);
  return date ? dateTimeFmt.format(date) : '—';
};

// 0.93 → "93 %"
export const formatRatio = (value) => (Number.isFinite(value) ? percentFmt.format(value) : '—');

// "2026-W37" → "Sem 37"
export const formatYearWeek = (value) => {
  const match = /W(\d+)/.exec(value ?? '');
  return match ? `Sem ${Number(match[1])}` : (value ?? '—');
};

// Today's date as YYYY-MM-DD for <input type="date">.
export const todayIso = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

// Counts items per key: [{ key, count }] sorted from highest to lowest.
export function countBy(items, keyFn) {
  const map = new Map();
  items.forEach((item) => {
    const key = keyFn(item);
    map.set(key, (map.get(key) ?? 0) + 1);
  });
  return [...map].map(([key, count]) => ({ key, count })).sort((a, b) => b.count - a.count);
}
