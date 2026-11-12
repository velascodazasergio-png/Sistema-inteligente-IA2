// Cliente HTTP único hacia Spring Boot.
// Regla de negocio: el frontend NUNCA llama a n8n, Qdrant ni OpenRouter;
// todas las peticiones pasan por aquí y van a la API REST del backend.

import { getApiBaseUrl, normalizeBaseUrl } from './config.js';

const DEFAULT_TIMEOUT_MS = 15000;

const STATUS_MESSAGES = {
  400: 'La solicitud tiene datos inválidos.',
  401: 'Tu sesión no es válida. Inicia sesión de nuevo.',
  403: 'No tienes permisos para realizar esta acción.',
  404: 'El recurso solicitado no existe.',
  409: 'La operación entra en conflicto con el estado actual.',
  422: 'Los datos enviados no son válidos.',
  500: 'Error interno del servidor.',
  502: 'El servidor no pudo comunicarse con un servicio externo.',
  503: 'El servicio no está disponible en este momento.',
  504: 'El servidor tardó demasiado en responder.',
};

export class ApiError extends Error {
  constructor(message, { status = 0, code = 'HTTP_ERROR', details = null, cause } = {}) {
    super(message, { cause });
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

export const isAbortError = (error) => error?.name === 'AbortError';

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

async function parseBody(response) {
  if (response.status === 204) return null;
  const type = response.headers.get('content-type') || '';
  if (type.includes('application/json')) return response.json();
  const text = await response.text();
  return text || null;
}

// Extrae un mensaje legible de las respuestas de error de Spring Boot.
function messageFrom(status, body) {
  if (body && typeof body === 'object') {
    if (Array.isArray(body.errors) && body.errors.length) {
      return body.errors.map((e) => e.defaultMessage || e.message || String(e)).join(' · ');
    }
    // GlobalExceptionHandler: errores de validación como { details: { campo: mensaje } }
    if (body.details && typeof body.details === 'object' && Object.keys(body.details).length) {
      return Object.values(body.details).join(' · ');
    }
    if (body.message) return body.message;
    if (body.detail) return body.detail;
  }
  return STATUS_MESSAGES[status] ?? `Error inesperado (HTTP ${status}).`;
}

export async function request(path, {
  method = 'GET',
  body,
  query,
  signal,
  timeout = DEFAULT_TIMEOUT_MS,
  headers = {},
} = {}) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeout);
  const forwardAbort = () => controller.abort();
  signal?.addEventListener('abort', forwardAbort, { once: true });

  let response;
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers: {
        Accept: 'application/json',
        ...(body !== undefined && { 'Content-Type': 'application/json' }),
        ...headers,
      },
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });
  } catch (error) {
    // Cancelada por quien llamó (p. ej. el usuario cambió de sección).
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
    throw new ApiError(messageFrom(response.status, data), { status: response.status, details: data });
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

// Comprueba si el backend responde. Usa /api-docs (springdoc), que ya existe.
// mode 'no-cors' permite saber si el servidor está levantado aunque aún no
// tenga CORS configurado; no sirve para leer datos.
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
