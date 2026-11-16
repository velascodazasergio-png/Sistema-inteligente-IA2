// Interface preferences of the logged-in user, stored in the database (table user_preferences).
// Contract: GET / PUT /api/v1/auth/me/preferences → { sidebarCollapsed, knowledgeView }

import { api } from '../api.js';

// Copy in memory of what the database returned (empty until loaded, and after logout)
let preferences = {};

/** Value of one preference, or undefined if the user never set it. */
export const getPreference = (name) => preferences[name] ?? undefined;

// Request in progress, so several callers share one load
let loading = null;

/** Loads the preferences of the logged-in user from the database. */
export function loadPreferences() {
  loading ??= api.get('/api/v1/auth/me/preferences')
    .then((data) => { preferences = data ?? {}; })
    .catch(() => { preferences = {}; })
    .then(() => {
      loading = null;
      window.dispatchEvent(new CustomEvent('preferences:loaded'));
      return preferences;
    });
  return loading;
}

/** Saves one preference in the database (the UI is not blocked while it saves). */
export function savePreference(name, value) {
  preferences = { ...preferences, [name]: value };
  return api.put('/api/v1/auth/me/preferences', { [name]: value }).catch(() => {
    // Not saved (no connection): it applies to this page only until the next load.
  });
}

/** Forgets the preferences when the user logs out. */
export function clearPreferences() {
  preferences = {};
}
