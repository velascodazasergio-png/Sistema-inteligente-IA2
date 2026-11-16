// User session. The session itself lives in the database (table user_sessions): the backend opens it
// at login, checks it on every request and closes it at logout. The browser only keeps the token,
// the key that identifies that session; the user data always comes from the database (GET /auth/me).
// The UI adapts to the role, but the backend always has the final word on permissions.

const TOKEN_KEY = 'incidentes-ia:token';
const LEGACY_KEYS = ['incidentes-ia:session', 'incidentes-ia:sidebar', 'incidentes-ia:kb-view', 'incidentes-ia:api-base-url'];

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

// Session in memory: { token, expiresAt, user }. user stays null until it is read from the database.
let cached;

// Notifies the app that the logged-in user changed
const emit = (user) => {
  if (typeof window !== 'undefined') window.dispatchEvent(new CustomEvent('auth:changed', { detail: { user } }));
};

// Earlier versions kept the whole session and the preferences in the browser: they are removed
function dropLegacyData() {
  try {
    LEGACY_KEYS.forEach((key) => localStorage.removeItem(key));
  } catch {
    // No storage: nothing to remove.
  }
}

// Expiration date written inside the JWT (exp claim), or null if it cannot be read
export function tokenExpiration(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return Number.isFinite(payload.exp) ? new Date(payload.exp * 1000).toISOString() : null;
  } catch {
    return null;
  }
}

// Token stored in this browser, as a session whose user is not known yet
function read() {
  dropLegacyData();
  try {
    const token = localStorage.getItem(TOKEN_KEY);
    return token ? { token, expiresAt: tokenExpiration(token), user: null } : null;
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

// Stores the session returned by the login: only the token goes to the browser storage
export function setSession({ accessToken, expiresAt, user }) {
  cached = { token: accessToken, expiresAt, user };
  try {
    localStorage.setItem(TOKEN_KEY, accessToken);
  } catch {
    // Storage blocked: the session lasts until the page is reloaded.
  }
  emit(user);
}

// Sets the user read from the database for the current token
export function setSessionUser(user) {
  const session = getSession();
  if (!session) return;
  session.user = user;
  emit(user);
}

// Deletes the session from this browser (silent: without notifying the app)
export function clearSession({ silent = false } = {}) {
  cached = null;
  try {
    localStorage.removeItem(TOKEN_KEY);
  } catch {
    // No storage: nothing to delete.
  }
  if (!silent) emit(null);
}

// Another tab logged in or out: this tab catches up (the storage event only reaches the other tabs)
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (event) => {
    if (event.key !== TOKEN_KEY && event.key !== null) return;
    cached = read();
    if (cached) window.dispatchEvent(new CustomEvent('auth:restore'));
    else emit(null);
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
