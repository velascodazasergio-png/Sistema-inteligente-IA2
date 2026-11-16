// Central frontend configuration. Nothing is stored in the browser: the API address is
// worked out from where the page is served.

// Served over HTTPS (nginx proxy), the page uses the API from its own origin: nginx forwards /api
// to the backend. That way there is no mixed content (HTTPS → HTTP) and no CORS is needed.
// In development (HTTP), the API is on port 8080 of the same machine.
const servedOverHttps = typeof location !== 'undefined' && location.protocol === 'https:';
export const DEFAULT_API_BASE_URL = servedOverHttps ? location.origin : 'http://localhost:8080';

// Trims the URL and removes the trailing slashes
export function normalizeBaseUrl(url) {
  return String(url).trim().replace(/\/+$/, '');
}

// API base URL
export function getApiBaseUrl() {
  return DEFAULT_API_BASE_URL;
}
