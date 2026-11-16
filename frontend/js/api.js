// Single HTTP client towards Spring Boot.
// Business rule: the frontend NEVER calls n8n, Qdrant or OpenRouter;
// every request goes through here to the backend REST API.

import { getApiBaseUrl, normalizeBaseUrl } from './config.js';
import { translateApiMessage } from './api-messages.js';
import { getToken, clearSession } from './auth.js';

const DEFAULT_TIMEOUT_MS = 15000;

const STATUS_MESSAGES = {
  400: 'La solicitud tiene datos inválidos.',
  401: 'Tu sesión no es válida. Inicia sesión de nuevo.',
  403: 'No tienes permisos para realizar esta acción.',
  404: 'El recurso solicitado no existe.',
  405: 'La operación no está permitida.',
  413: 'Los datos enviados son demasiado grandes.',
  415: 'El formato de los datos enviados no es compatible.',
  409: 'La operación entra en conflicto con el estado actual.',
  422: 'Los datos enviados no son válidos.',
  500: 'Error interno del servidor.',
  502: 'El servidor no pudo comunicarse con un servicio externo.',
  503: 'El servicio no está disponible en este momento.',
  504: 'El servidor tardó demasiado en responder.',
};

// HTTP error with status, code, details and per-field errors, ready to show in the UI
export class ApiError extends Error {
  // Builds the error with its message and HTTP details
  constructor(message, { status = 0, code = 'HTTP_ERROR', details = null, fieldErrors = {}, cause } = {}) {
    super(message, { cause });
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = details;
    // Per-field validation errors, already translated: { title: 'El título es obligatorio.' }
    this.fieldErrors = fieldErrors;
  }
}

// true if the request was cancelled (not a real error)
export const isAbortError = (error) => error?.name === 'AbortError';

// Full API URL with the query string (empty values are skipped)
function buildUrl(path, query) {
  const cleanPath = path.startsWith('/') ? path : `/${path}`;
  const url = new URL(`${getApiBaseUrl()}${cleanPath}`);
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, value);
      }
    }
  }
  return url;
}

// Reads the response body as JSON, text or nothing (204)
async function parseBody(response) {
  if (response.status === 204) return null;
  const type = response.headers.get('content-type') || '';
  if (type.includes('application/json')) return response.json();
  const text = await response.text();
  return text || null;
}

// Per-field validation errors ({ details: { field: message } } from GlobalExceptionHandler), translated.
function fieldErrorsFrom(body) {
  if (!body || typeof body !== 'object' || !body.details || typeof body.details !== 'object') return {};
  return Object.fromEntries(Object.entries(body.details).map(([field, message]) => [field, translateApiMessage(message) ?? message]));
}

// Readable Spanish message for a Spring Boot error response.
// - 400 validation: the message of each field.
// - 400/404/409 with a backend message: its translation (or the original if unknown).
// - 405, 415, 5xx and others: a generic message for the code, without English technical details.
function messageFrom(status, body, fieldErrors) {
  const fields = Object.values(fieldErrors);
  if (fields.length) return fields.join(' · ');
  if (body && typeof body === 'object') {
    const raw = body.message || body.detail;
    const translated = translateApiMessage(raw);
    if (translated) return translated;
    if (raw && [400, 404, 409].includes(status)) return raw;
  }
  return STATUS_MESSAGES[status] ?? `Error inesperado (HTTP ${status}).`;
}

// Sends a request to the backend with JWT, timeout and cancellation; throws ApiError on failure
export async function request(path, {
  method = 'GET',
  body,
  query,
  signal,
  timeout = DEFAULT_TIMEOUT_MS,
  headers = {},
  auth = true,
} = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeout);
  // Cancels this request when the caller's signal is aborted
  const forwardAbort = () => controller.abort();
  signal?.addEventListener('abort', forwardAbort, { once: true });

  // With a session, every request carries the JWT. Login (auth: false) does not need it.
  const token = auth ? getToken() : null;

  let response;
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined && { 'Content-Type': 'application/json' }),
        ...(token && { Authorization: `Bearer ${token}` }),
        ...headers,
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });
  } catch (error) {
    // Cancelled by the caller (e.g. the user moved to another section).
    if (signal?.aborted) throw error;
    if (controller.signal.aborted) {
      throw new ApiError('El servidor tardó demasiado en responder.', { code: 'TIMEOUT', cause: error });
    }
    throw new ApiError(
      'No se pudo conectar con el servidor. Verifica que el backend esté en ejecución y permita CORS.',
      { code: 'NETWORK_ERROR', cause: error },
    );
  } finally {
    clearTimeout(timer);
    signal?.removeEventListener('abort', forwardAbort);
  }

  const data = await parseBody(response).catch(() => null);
  if (!response.ok) {
    // Expired or invalid token: the session is closed and the app goes back to the login screen
    if (response.status === 401 && token) {
      clearSession();
      if (typeof window !== 'undefined') window.dispatchEvent(new CustomEvent('auth:expired'));
    }
    const fieldErrors = fieldErrorsFrom(data);
    throw new ApiError(messageFrom(response.status, data, fieldErrors), { status: response.status, details: data, fieldErrors });
  }
  return data;
}

export const api = {
  get: (path, options) => request(path, { ...options, method: 'GET' }),
  post: (path, body, options) => request(path, { ...options, method: 'POST', body }),
  put: (path, body, options) => request(path, { ...options, method: 'PUT', body }),
  patch: (path, body, options) => request(path, { ...options, method: 'PATCH', body }),
  delete: (path, options) => request(path, { ...options, method: 'DELETE' }),
};

// Checks whether the backend answers. Uses /api-docs (springdoc), which always exists.
// mode 'no-cors' tells whether the server is up even if CORS is not
// configured yet; it cannot be used to read data.
export async function checkHealth(baseUrl = getApiBaseUrl(), timeout = 4000) {
  try {
    await fetch(`${normalizeBaseUrl(baseUrl)}/api-docs`, {
      mode: 'no-cors',
      cache: 'no-store',
      signal: AbortSignal.timeout(timeout),
    });
    return true;
  } catch {
    return false;
  }
}
