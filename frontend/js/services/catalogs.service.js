// Catalogs for forms and filters: categories and users.
// Contract: backend/src/main/java/com/mafe/incidentai/controller/CatalogController.java

import { api } from '../api.js';

/** GET /api/v1/categories — CategoryResponseDTO[] { id, name, description, active } */
export const listCategories = ({ activeOnly = false } = {}, options) =>
  api.get('/api/v1/categories', { ...options, query: { activeOnly: activeOnly || undefined } });

/** GET /api/v1/users — UserSummaryDTO[] { id, fullName, email, role } */
export const listUsers = ({ role } = {}, options) =>
  api.get('/api/v1/users', { ...options, query: { role } });

// { id, fullName } → { id, name } for the <select> elements
export const toOptions = (users) => users.map((u) => ({ id: u.id, name: u.fullName }));

// Labels of the UserRole enum
export const ROLES = {
  EMPLOYEE: 'Empleado',
  SUPPORT_TECHNICIAN: 'Técnico de soporte',
  ADMIN: 'Administrador',
};
