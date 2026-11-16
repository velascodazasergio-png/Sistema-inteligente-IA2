// Validation errors returned by the backend (400), shown next to each field
// with the same look as the browser validation.

/**
 * @param {HTMLFormElement} form
 * @param {{ fieldErrors?: Record<string, string> }} error  ApiError from the HTTP client.
 * @param {(field: string) => string} errorIdFor  id of the <p class="field__error"> of each field.
 * @returns {boolean} true if at least one error was placed next to its field.
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

/** "n/max" counter for a field with maxlength. */
export function attachCounter(field, output) {
  const max = Number(field.getAttribute('maxlength'));
  // Refreshes the "n/max" counter
  const update = () => { output.textContent = `${field.value.length}/${max}`; };
  field.addEventListener('input', update);
  update();
}
