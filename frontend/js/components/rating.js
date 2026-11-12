// Calificación de 1 a 5 estrellas (RF08).
// Usa radios nativos: funciona con teclado (flechas) y lectores de pantalla
// sin código extra. El envío al backend lo hace la vista que lo use.

import { icon, escapeHtml } from '../ui.js';

let uid = 0;

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
    setDisabled(disabled) {
      fieldset.disabled = disabled;
    },
  };
}

// Versión de solo lectura para tablas y listados.
export function starsHtml(value) {
  const rounded = Math.round(value ?? 0);
  return `
    <span class="stars" role="img" aria-label="${rounded} de 5 estrellas">
      ${[1, 2, 3, 4, 5].map((n) => `<span class="${n <= rounded ? 'is-on' : ''}">${icon('i-star')}</span>`).join('')}
    </span>`;
}
