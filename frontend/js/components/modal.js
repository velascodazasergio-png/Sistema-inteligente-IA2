// Modal based on the native <dialog>: the browser already handles focus
// trapping, the Escape key and blocking the background. This adds
// enter/exit animations and returns the focus on close.

import { icon, escapeHtml } from '../ui.js';

let uid = 0;

/**
 * @param {object} o
 * @param {string} o.title
 * @param {string|Node} [o.body]  Already escaped HTML or a DOM node.
 * @param {Array<{label:string, variant?:'primary'|'ghost'|'danger', value?:any,
 *   autofocus?:boolean, onClick?:(ctx:{close:Function})=>void}>} [o.actions]
 * @param {'sm'|'md'|'lg'} [o.size]
 * @param {(value:any)=>void} [o.onClose]
 */
export function openModal({ title, body = '', actions = [], size = 'md', onClose } = {}) {
  const previousFocus = document.activeElement;
  const titleId = `modal-title-${++uid}`;

  const dialog = document.createElement('dialog');
  dialog.className = `modal modal--${size}`;
  dialog.setAttribute('aria-labelledby', titleId);
  dialog.innerHTML = `
    <header class="modal__header">
      <h2 class="modal__title" id="${titleId}">${escapeHtml(title)}</h2>
      <button class="icon-btn" type="button" data-close aria-label="Cerrar">${icon('i-close')}</button>
    </header>
    <div class="modal__body"></div>
    ${actions.length ? '<footer class="modal__footer"></footer>' : ''}`;

  const bodyEl = dialog.querySelector('.modal__body');
  if (typeof body === 'string') bodyEl.innerHTML = body;
  else bodyEl.append(body);

  let closing = false;
  let result;

  // Closes the dialog with the exit animation and returns the value
  function close(value) {
    if (closing) return;
    closing = true;
    result = value;
    dialog.classList.add('modal--closing');
    // Really closes the <dialog> once the animation ends
    const finish = () => dialog.open && dialog.close();
    dialog.addEventListener('animationend', finish, { once: true });
    setTimeout(finish, 320); // fallback if there is no animation
  }

  const footer = dialog.querySelector('.modal__footer');
  actions.forEach((action) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `btn btn--${action.variant ?? 'ghost'}`;
    button.textContent = action.label;
    if (action.autofocus) button.setAttribute('autofocus', '');
    button.addEventListener('click', () => (action.onClick ? action.onClick({ close }) : close(action.value)));
    footer.append(button);
  });

  dialog.querySelector('[data-close]').addEventListener('click', () => close());
  // Escape: intercepted to close with the animation.
  dialog.addEventListener('cancel', (event) => {
    event.preventDefault();
    close();
  });
  // Click on the dark backdrop (outside the content).
  dialog.addEventListener('click', (event) => {
    if (event.target === dialog) close();
  });
  dialog.addEventListener('close', () => {
    dialog.remove();
    previousFocus?.focus?.();
    onClose?.(result);
  });

  document.body.append(dialog);
  dialog.showModal();

  return { element: dialog, body: bodyEl, close };
}

// Yes/no confirmation. Returns a promise with true or false.
export function confirmDialog({ title, message, confirmLabel = 'Confirmar', cancelLabel = 'Cancelar', danger = false }) {
  return new Promise((resolve) => {
    openModal({
      title,
      size: 'sm',
      body: `<p>${escapeHtml(message)}</p>`,
      actions: [
        { label: cancelLabel, value: false },
        { label: confirmLabel, value: true, variant: danger ? 'danger' : 'primary', autofocus: true },
      ],
      onClose: (value) => resolve(value === true),
    });
  });
}
