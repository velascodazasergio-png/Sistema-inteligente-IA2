// Catálogos para formularios y filtros: categorías y usuarios.
// Contrato: backend/src/main/java/com/mafe/incidentai/controller/CatalogController.java

import { api } from '../api.js';

/** GET /api/v1/categories — CategoryResponseDTO[] { id, name, description, active } */
export const listCategories = ({ activeOnly = false } = {}, options) =>
  api.get('/api/v1/categories', { ...options, query: { activeOnly: activeOnly || undefined } });

/** GET /api/v1/users — UserSummaryDTO[] { id, fullName, email, role } */
export const listUsers = ({ role } = {}, options) =>
  api.get('/api/v1/users', { ...options, query: { role } });

// { id, fullName } → { id, name } para los <select>
export const toOptions = (users) => users.map((u) => ({ id: u.id, name: u.fullName }));

// Etiquetas del enum UserRole
export const ROLES = {
  EMPLOYEE: 'Empleado',
  SUPPORT_TECHNICIAN: 'Técnico de soporte',
  ADMIN: 'Administrador',
};
