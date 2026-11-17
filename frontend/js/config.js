// Central frontend configuration. Nothing is stored in the browser: the API address is
// worked out from where the page is served.

// Served by nginx (HTTP or HTTPS), the page uses the API from its own origin: nginx forwards /api
// to the backend. That way there is no mixed content (HTTPS → HTTP) and no CORS is needed.
// Opened as a local file, the API is on port 8081 of the same machine.
const servedOverHttp = typeof location !== 'undefined' && /^https?:$/.test(location.protocol);
export const DEFAULT_API_BASE_URL = servedOverHttp ? location.origin : 'http://localhost:8081';

// Trims the URL and removes the trailing slashes
export function normalizeBaseUrl(url) {
  return String(url).trim().replace(/\/+$/, '');
}

// API base URL
export function getApiBaseUrl() {
  return DEFAULT_API_BASE_URL;
}
