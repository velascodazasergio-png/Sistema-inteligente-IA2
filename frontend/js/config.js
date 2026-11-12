// Configuración central del frontend.
// La URL base de la API se puede cambiar desde la vista "Configuración";
// el valor se guarda solo en este navegador (localStorage).

const STORAGE_KEY = 'incidentes-ia:api-base-url';

export const DEFAULT_API_BASE_URL = 'http://localhost:8080';

export function normalizeBaseUrl(url) {
  return String(url).trim().replace(/\/+$/, '');
}

export function getApiBaseUrl() {
  try {
    return localStorage.getItem(STORAGE_KEY) || DEFAULT_API_BASE_URL;
  } catch {
    return DEFAULT_API_BASE_URL;
  }
}

export function setApiBaseUrl(url) {
  const clean = normalizeBaseUrl(url);
  try {
    localStorage.setItem(STORAGE_KEY, clean);
  } catch {
    // Almacenamiento bloqueado (modo privado): se usará el valor por defecto.
  }
  return clean;
}

export function resetApiBaseUrl() {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Sin almacenamiento disponible: no hay nada que borrar.
  }
}
