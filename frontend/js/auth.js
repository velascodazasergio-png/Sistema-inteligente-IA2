// Sesión del usuario: el JWT que entrega POST /api/v1/auth/login y los datos de quien inició sesión.
// Se guarda en este navegador (localStorage) hasta que vence o se cierra la sesión.
// La interfaz se adapta al rol, pero quien decide los permisos es siempre el backend.

const STORAGE_KEY = 'incidentes-ia:session';

export const ROLE = {
  ADMIN: 'ADMIN',
  TECH: 'SUPPORT_TECHNICIAN',
  EMPLOYEE: 'EMPLOYEE',
};

export const ROLE_LABEL = {
  ADMIN: 'Administrador',
  SUPPORT_TECHNICIAN: 'Técnico de soporte',
  EMPLOYEE: 'Usuario',
};

// Sección de inicio de cada rol
const HOME = {
  ADMIN: 'dashboard',
  SUPPORT_TECHNICIAN: 'dashboard',
  EMPLOYEE: 'incidentes',
};

let cached;

const emit = (user) => {
  if (typeof window !== 'undefined') window.dispatchEvent(new CustomEvent('auth:changed', { detail: { user } }));
};

function read() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
  } catch {
    return null;
  }
}

export function isExpired(session, now = Date.now()) {
  const expiresAt = Date.parse(session?.expiresAt ?? '');
  return !session?.token || Number.isNaN(expiresAt) || expiresAt <= now;
}

/** Sesión vigente o null (una sesión vencida se borra). */
export function getSession() {
  if (cached === undefined) cached = read();
  if (cached && isExpired(cached)) clearSession({ silent: true });
  return cached;
}

export function setSession({ accessToken, expiresAt, user }) {
  cached = { token: accessToken, expiresAt, user };
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(cached));
  } catch {
    // Almacenamiento bloqueado: la sesión dura hasta recargar la página.
  }
  emit(user);
}

export function clearSession({ silent = false } = {}) {
  cached = null;
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Sin almacenamiento: no hay nada que borrar.
  }
  if (!silent) emit(null);
}

// Otra pestaña inició o cerró sesión: esta se pone al día (el evento storage solo llega a las demás pestañas)
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (event) => {
    if (event.key !== STORAGE_KEY && event.key !== null) return;
    cached = read();
    emit(cached?.user ?? null);
  });
}

export const getToken = () => getSession()?.token ?? null;
export const currentUser = () => getSession()?.user ?? null;
export const hasRole = (...roles) => roles.includes(currentUser()?.role);
export const isAdmin = () => hasRole(ROLE.ADMIN);
export const isEmployee = () => hasRole(ROLE.EMPLOYEE);
export const isStaff = () => hasRole(ROLE.ADMIN, ROLE.TECH);

export const homeFor = (role) => HOME[role] ?? 'login';

/** ¿Puede el rol abrir esta sección? Las rutas sin "roles" son para cualquier sesión. */
export function canAccess(route, role = currentUser()?.role) {
  if (route.public) return true;
  if (!role) return false;
  return !route.roles || route.roles.includes(role);
}
