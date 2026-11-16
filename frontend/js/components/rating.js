// 1 to 5 star rating (RF08).
// Uses native radios: works with the keyboard (arrows) and screen readers
// with no extra code. Sending it to the backend is up to the view that uses it.

import { icon, escapeHtml } from '../ui.js';

let uid = 0;

// Interactive 1–5 star control; returns its current value and setDisabled
export function createStarRating(container, { value = 0, label = 'Calificación', onChange, readonly = false } = {}) {
  const name = `rating-${++uid}`;
  container.innerHTML = `
    <fieldset class="rating${readonly ? ' rating--readonly' : ''}" ${readonly ? 'disabled' : ''}>
      <legend class="sr-only">${escapeHtml(label)}</legend>
      ${[1, 2, 3, 4, 5].map((n) => `
        <input class="sr-only" type="radio" id="${name}-${n}" name="${name}" value="${n}" ${n === value ? 'checked' : ''}>
        <label for="${name}-${n}" title="${n} de 5">
          ${icon('i-star')}<span class="sr-only">${n} ${n === 1 ? 'estrella' : 'estrellas'}</span>
        </label>`).join('')}
    </fieldset>`;

  const fieldset = container.querySelector('fieldset');
  fieldset.addEventListener('change', (event) => onChange?.(Number(event.target.value)));

  return {
    get value() {
      return Number(fieldset.querySelector('input:checked')?.value ?? 0);
    },
    // Enables or disables the stars
    setDisabled(disabled) {
      fieldset.disabled = disabled;
    },
  };
}

// Read-only version for tables and lists.
export function starsHtml(value) {
  const rounded = Math.round(value ?? 0);
  return `
    <span class="stars" role="img" aria-label="${rounded} de 5 estrellas">
      ${[1, 2, 3, 4, 5].map((n) => `<span class="${n <= rounded ? 'is-on' : ''}">${icon('i-star')}</span>`).join('')}
    </span>`;
}
