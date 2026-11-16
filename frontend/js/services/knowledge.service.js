// Knowledge base service (RF02).
// Contract: backend/src/main/java/com/mafe/incidentai/controller/KnowledgeArticleController.java

import { api } from '../api.js';

const BASE = '/api/v1/knowledge-articles';

// Empty filter → not sent
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

// GET /{id} — one article
export const getArticle = (id, options) => api.get(`${BASE}/${id}`, options);

// The backend builds "content" (what n8n indexes) from these sections.
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

/** POST — the article starts active and unverified. */
export const createArticle = (data, options) => api.post(BASE, body(data), options);

/** PUT — if the title or the content changes, the backend marks it as unverified. */
export const updateArticle = (id, data, options) => api.put(`${BASE}/${id}`, body(data), options);

/** PATCH /{id}/status — { active?, verified? } */
export const updateArticleStatus = (id, changes, options) => api.patch(`${BASE}/${id}/status`, changes, options);
