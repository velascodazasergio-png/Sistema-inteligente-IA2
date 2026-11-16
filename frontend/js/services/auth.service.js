// Login, logout and session restore. The session is stored in the database (table user_sessions).
// Contract: backend/src/main/java/com/mafe/incidentai/controller/AuthController.java

import { api } from '../api.js';
import { setSession, setSessionUser, clearSession, getToken, currentUser } from '../auth.js';

/** POST /api/v1/auth/login → { accessToken, tokenType, expiresAt, user }; the backend opens the session. */
export async function login(email, password, options) {
  const session = await api.post('/api/v1/auth/login', { email: email.trim(), password }, { ...options, auth: false });
  setSession(session);
  return session.user;
}

/** POST /api/v1/auth/logout: closes the session in the database, then forgets the token here. */
export async function logout() {
  try {
    if (getToken()) await api.post('/api/v1/auth/logout');
  } catch {
    // Already closed or expired on the server: nothing else to do.
  }
  clearSession();
}

/** On page load: if there is a token, reads the user from the database (GET /auth/me). */
export async function restoreSession() {
  if (!getToken()) return null;
  if (currentUser()) return currentUser();
  try {
    const user = await api.get('/api/v1/auth/me');
    setSessionUser(user);
    return user;
  } catch {
    // Session closed or expired in the database (the HTTP client already cleared it) or no connection.
    clearSession({ silent: true });
    return null;
  }
}
