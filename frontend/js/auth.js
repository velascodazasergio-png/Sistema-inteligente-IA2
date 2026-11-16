// User session: the JWT returned by POST /api/v1/auth/login and the data of the logged-in user.
// It is kept in this browser (localStorage) until it expires or the user logs out.
// The UI adapts to the role, but the backend always has the final word on permissions.

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

// Home section of each role
const HOME = {
  ADMIN: 'dashboard',
  SUPPORT_TECHNICIAN: 'dashboard',
  EMPLOYEE: 'incidentes',
};

let cached;

// Notifies the app that the logged-in user changed
const emit = (user) => {
  if (typeof window !== 'undefined') window.dispatchEvent(new CustomEvent('auth:changed', { detail: { user } }));
};

// Session stored in this browser, or null
function read() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null');
  } catch {
    return null;
  }
}

// true if the session has no token or its expiration date has passed
export function isExpired(session, now = Date.now()) {
  const expiresAt = Date.parse(session?.expiresAt ?? '');
  return !session?.token || Number.isNaN(expiresAt) || expiresAt <= now;
}

/** Current session, or null (an expired session is deleted). */
export function getSession() {
  if (cached === undefined) cached = read();
  if (cached && isExpired(cached)) clearSession({ silent: true });
  return cached;
}

// Stores the session returned by the login (memory + localStorage)
export function setSession({ accessToken, expiresAt, user }) {
  cached = { token: accessToken, expiresAt, user };
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(cached));
  } catch {
    // Storage blocked: the session lasts until the page is reloaded.
  }
  emit(user);
}

// Deletes the session (silent: without notifying the app)
export function clearSession({ silent = false } = {}) {
  cached = null;
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // No storage: nothing to delete.
  }
  if (!silent) emit(null);
}

// Another tab logged in or out: this tab catches up (the storage event only reaches the other tabs)
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (event) => {
    if (event.key !== STORAGE_KEY && event.key !== null) return;
    cached = read();
    emit(cached?.user ?? null);
  });
}

// JWT of the current session, or null
export const getToken = () => getSession()?.token ?? null;
// User of the current session, or null
export const currentUser = () => getSession()?.user ?? null;
// true if the current user has one of the given roles
export const hasRole = (...roles) => roles.includes(currentUser()?.role);
// true for administrators
export const isAdmin = () => hasRole(ROLE.ADMIN);
// true for employees (the "Usuario" role)
export const isEmployee = () => hasRole(ROLE.EMPLOYEE);
// true for support staff (administrator or technician)
export const isStaff = () => hasRole(ROLE.ADMIN, ROLE.TECH);

// Home section of a role (login when there is no role)
export const homeFor = (role) => HOME[role] ?? 'login';

/** Can the role open this section? Routes without "roles" are open to any session. */
export function canAccess(route, role = currentUser()?.role) {
  if (route.public) return true;
  if (!role) return false;
  return !route.roles || route.roles.includes(role);
}
