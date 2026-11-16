// Login (JWT).
// Contract: backend/src/main/java/com/mafe/incidentai/controller/AuthController.java

import { api } from '../api.js';
import { setSession, clearSession } from '../auth.js';

/** POST /api/v1/auth/login → { accessToken, tokenType, expiresAt, user } and stores the session. */
export async function login(email, password, options) {
  const session = await api.post('/api/v1/auth/login', { email: email.trim(), password }, { ...options, auth: false });
  setSession(session);
  return session.user;
}

// Logs out (deletes the session)
export function logout() {
  clearSession();
}
