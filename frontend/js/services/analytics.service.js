// Servicio de analítica (RF06): indicadores calculados con consultas en el backend.
// Contrato: backend/src/main/java/com/mafe/incidentai/controller/AnalyticsController.java
// Todos aceptan los filtros opcionales { categoryId, priority, startDate, endDate } (fechas AAAA-MM-DD).

import { api } from '../api.js';
import { startOfDay, endOfDay } from './incidents.service.js';

const BASE = '/api/v1/analytics';

const query = (f = {}) => ({
  categoryId: f.categoryId || undefined,
  priority: f.priority || undefined,
  startDate: startOfDay(f.startDate),
  endDate: endOfDay(f.endDate),
});

/** AnalyticsDashboardDTO: totales, % de resolución, horas promedio, calificación IA, top categorías y tendencia semanal. */
export const getDashboard = (filters, options) => api.get(`${BASE}/dashboard`, { ...options, query: query(filters) });

/** CategoryMetricDTO[] ordenado de mayor a menor. */
export const getTopCategories = (filters, options) => api.get(`${BASE}/top-categories`, { ...options, query: query(filters) });

/** WeeklyTrendDTO[]: { yearWeek: 'AAAA-WNN', totalReported, totalResolved } */
export const getWeeklyTrends = (filters, options) => api.get(`${BASE}/weekly-trends`, { ...options, query: query(filters) });
