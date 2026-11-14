// Inicio de sesión: correo y contraseña → JWT (POST /api/v1/auth/login).
// Tras entrar, cada rol llega a su propia sección de inicio.

import { icon, escapeHtml, toast, setBusy } from '../ui.js';
import { login } from '../services/auth.service.js';
import { homeFor, ROLE_LABEL } from '../auth.js';
import { routeParams, navigate } from '../router.js';
import { showServerFieldErrors } from '../components/form-errors.js';

// Usuarios de data.sql, uno por rol, para probar cada interfaz
const DEMO_ACCOUNTS = [
  { role: 'ADMIN', name: 'Sarah Jenkins', email: 'sarah.jenkins@enterprise.io', what: 'Todo el sistema, verificación de artículos y configuración.' },
  { role: 'SUPPORT_TECHNICIAN', name: 'Marcus Vance', email: 'marcus.vance@enterprise.io', what: 'Atiende incidentes, redacta artículos y consulta la IA.' },
  { role: 'EMPLOYEE', name: 'Laura Chen', email: 'laura.chen@enterprise.io', what: 'Reporta sus incidentes y sigue su estado.' },
];

const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const RULES = {
  email: (v) => (!v ? 'Escribe tu correo.' : !EMAIL.test(v) ? 'Escribe un correo válido.' : ''),
  password: (v) => (v ? '' : 'Escribe tu contraseña.'),
};

export async function render(container) {
  container.innerHTML = `
    <div class="auth">
      <section class="auth__intro" aria-labelledby="auth-title">
        <span class="brand-mark brand-mark--lg" aria-hidden="true">${icon('i-logo')}</span>
        <p class="auth__eyebrow">Centro de soporte · Incidentes IA</p>
        <h1 class="auth__title" id="auth-title">Cada rol, su propio espacio de trabajo</h1>
        <p class="auth__lead">Inicia sesión con tu correo corporativo. Verás solo las secciones y acciones que corresponden a tu rol.</p>
        <ul class="auth__roles" aria-label="Cuentas de demostración">
          ${DEMO_ACCOUNTS.map((a) => `
            <li>
              <button class="auth-role auth-role--${a.role.toLowerCase()}" type="button" data-email="${escapeHtml(a.email)}">
                <span class="auth-role__head"><strong>${ROLE_LABEL[a.role]}</strong><span>${escapeHtml(a.name)}</span></span>
                <span class="auth-role__what">${escapeHtml(a.what)}</span>
                <span class="auth-role__email">${escapeHtml(a.email)}</span>
              </button>
            </li>`).join('')}
        </ul>
        <p class="auth__hint">${icon('i-info')}<span>Elige una cuenta para completar el correo. La contraseña de demostración está en el README (<code>DEMO_PASSWORD</code>).</span></p>
      </section>

      <form class="card auth__card" id="login-form" novalidate aria-labelledby="login-title">
        <h2 class="card__title" id="login-title">Iniciar sesión</h2>
        <p class="card__subtitle">Tu sesión dura una jornada de trabajo (8 horas).</p>
        <div class="field">
          <label for="l-email">Correo</label>
          <input class="input" id="l-email" name="email" type="email" autocomplete="username" maxlength="120"
                 placeholder="nombre@enterprise.io" aria-describedby="l-email-error" required>
          <p class="field__error" id="l-email-error" aria-live="polite"></p>
        </div>
        <div class="field">
          <label for="l-password">Contraseña</label>
          <div class="input-group">
            <input class="input" id="l-password" name="password" type="password" autocomplete="current-password" maxlength="72"
                   aria-describedby="l-password-error" required>
            <button class="icon-btn icon-btn--sm" type="button" data-toggle-password aria-pressed="false" aria-label="Mostrar contraseña" title="Mostrar contraseña">${icon('i-eye')}</button>
          </div>
          <p class="field__error" id="l-password-error" aria-live="polite"></p>
        </div>
        <p class="note note--danger auth__error" id="l-error" role="alert" hidden></p>
        <button class="btn btn--primary auth__submit" type="submit">${icon('i-arrow-right')}Entrar</button>
      </form>
    </div>`;

  const form = container.querySelector('#login-form');
  const formError = container.querySelector('#l-error');
  const touched = new Set();

  const validateField = (name) => {
    const field = form.elements[name];
    const value = name === 'email' ? field.value.trim() : field.value;
    const message = RULES[name](value);
    form.querySelector(`#l-${name}-error`).textContent = message;
    field.setAttribute('aria-invalid', String(Boolean(message)));
    return !message;
  };

  Object.keys(RULES).forEach((name) => {
    const field = form.elements[name];
    field.addEventListener('blur', () => { touched.add(name); validateField(name); });
    field.addEventListener('input', () => {
      formError.hidden = true;
      if (touched.has(name)) validateField(name);
    });
  });

  const toggle = form.querySelector('[data-toggle-password]');
  toggle.addEventListener('click', () => {
    const show = form.elements.password.type === 'password';
    form.elements.password.type = show ? 'text' : 'password';
    toggle.setAttribute('aria-pressed', String(show));
    const label = show ? 'Ocultar contraseña' : 'Mostrar contraseña';
    toggle.setAttribute('aria-label', label);
    toggle.title = label;
  });

  container.querySelectorAll('[data-email]').forEach((button) => button.addEventListener('click', () => {
    form.elements.email.value = button.dataset.email;
    validateField('email');
    form.elements.password.focus();
  }));

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    const valid = Object.keys(RULES).map((name) => { touched.add(name); return validateField(name); });
    if (valid.includes(false)) {
      form.querySelector('[aria-invalid="true"]')?.focus();
      return;
    }
    const button = form.querySelector('[type="submit"]');
    setBusy(button, true, 'Entrando…');
    formError.hidden = true;
    try {
      const user = await login(form.elements.email.value, form.elements.password.value);
      toast(`Hola, ${user.fullName}. Entraste como ${ROLE_LABEL[user.role] ?? user.role}.`, { type: 'success' });
      // Vuelve a la sección que pidió antes de iniciar sesión (si su rol puede verla; si no, el router redirige)
      navigate(routeParams().get('next') || homeFor(user.role));
    } catch (error) {
      setBusy(button, false);
      if (showServerFieldErrors(form, error, (name) => `l-${name}-error`)) return;
      formError.textContent = error.message;
      formError.hidden = false;
      if (error.status === 401) {
        form.elements.password.value = '';
        form.elements.password.focus();
      }
    }
  });

  form.elements.email.focus();
}
