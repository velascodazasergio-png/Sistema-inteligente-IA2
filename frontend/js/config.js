// Central frontend configuration.
// The API base URL can be changed from the "Configuración" view;
// the value is only stored in this browser (localStorage).

const STORAGE_KEY = 'incidentes-ia:api-base-url';

// Served over HTTPS (nginx proxy), the page uses the API from its own origin: nginx forwards /api
// to the backend. That way there is no mixed content (HTTPS → HTTP) and no CORS is needed.
// In development (HTTP), the API is on port 8080.
const servedOverHttps = typeof location !== 'undefined' && location.protocol === 'https:';
export const DEFAULT_API_BASE_URL = servedOverHttps ? location.origin : 'http://localhost:8080';

// Trims the URL and removes the trailing slashes
export function normalizeBaseUrl(url) {
  return String(url).trim().replace(/\/+$/, '');
}

// API base URL (saved value or default)
export function getApiBaseUrl() {
  try {
    return localStorage.getItem(STORAGE_KEY) || DEFAULT_API_BASE_URL;
  } catch {
    return DEFAULT_API_BASE_URL;
  }
}

// Saves a new API base URL in this browser
export function setApiBaseUrl(url) {
  const clean = normalizeBaseUrl(url);
  try {
    localStorage.setItem(STORAGE_KEY, clean);
  } catch {
    // Storage blocked (private mode): the default value is used.
  }
  return clean;
}

// Goes back to the default API base URL
export function resetApiBaseUrl() {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // No storage available: nothing to delete.
  }
}
