// Servicio de la base de conocimiento (RF02).
// Contrato: backend/src/main/java/com/mafe/incidentai/controller/KnowledgeArticleController.java

import { api } from '../api.js';

const BASE = '/api/v1/knowledge-articles';

const flag = (value) => (value === '' || value == null ? undefined : value);

/**
 * GET — filtros opcionales: q (texto), categoryId, active, verified ('true' | 'false').
 * @returns {Promise<KnowledgeArticleResponseDTO[]>}
 */
export const searchArticles = (filters = {}, options) => api.get(BASE, {
  ...options,
  query: {
    q: filters.q?.trim() || undefined,
    categoryId: filters.categoryId || undefined,
    active: flag(filters.active),
    verified: flag(filters.verified),
  },
});

export const getArticle = (id, options) => api.get(`${BASE}/${id}`, options);

// El backend arma "content" (lo que indexa n8n) a partir de estas secciones.
const body = ({ title, problem, symptoms, cause, solution, referenceUrl, categoryId, authorUserId }) => ({
  title: title.trim(),
  problem: problem.trim(),
  symptoms: symptoms?.trim() || null,
  cause: cause?.trim() || null,
  solution: solution.trim(),
  referenceUrl: referenceUrl?.trim() || null,
  categoryId: Number(categoryId),
  authorUserId: authorUserId ? Number(authorUserId) : null,
});

/** POST — el artículo nace activo y sin verificar. */
export const createArticle = (data, options) => api.post(BASE, body(data), options);

/** PUT — si cambia el título o el contenido, el backend lo deja sin verificar. */
export const updateArticle = (id, data, options) => api.put(`${BASE}/${id}`, body(data), options);

/** PATCH /{id}/status — { active?, verified? } */
export const updateArticleStatus = (id, changes, options) => api.patch(`${BASE}/${id}/status`, changes, options);
