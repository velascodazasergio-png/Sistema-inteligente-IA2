// Errores de validación que devuelve el backend (400), mostrados junto a cada campo
// con el mismo aspecto que la validación del navegador.

/**
 * @param {HTMLFormElement} form
 * @param {{ fieldErrors?: Record<string, string> }} error  ApiError del cliente HTTP.
 * @param {(field: string) => string} errorIdFor  id del <p class="field__error"> de cada campo.
 * @returns {boolean} true si algún error quedó junto a su campo.
 */
export function showServerFieldErrors(form, error, errorIdFor) {
  let first = null;
  for (const [name, message] of Object.entries(error?.fieldErrors ?? {})) {
    const holder = form.querySelector(`#${CSS.escape(errorIdFor(name))}`);
    if (!holder) continue;
    holder.textContent = message;
    const field = form.elements[name];
    if (field && typeof field.setAttribute === 'function') {
      field.setAttribute('aria-invalid', 'true');
      first ??= field;
    }
  }
  first?.focus();
  return Boolean(first);
}

/** Contador "n/máx" para un campo con maxlength. */
export function attachCounter(field, output) {
  const max = Number(field.getAttribute('maxlength'));
  const update = () => { output.textContent = `${field.value.length}/${max}`; };
  field.addEventListener('input', update);
  update();
}
