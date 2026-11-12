// Servicio de analítica (RF06): indicadores calculados con SQL en el backend.
// Contrato: backend/src/main/java/com/mafe/incidentai/controller/AnalyticsController.java

import { api } from '../api.js';

const BASE = '/api/v1/analytics';

/** AnalyticsDashboardDTO: totales, % de resolución, horas promedio, calificación IA, top categorías y tendencia semanal. */
export const getDashboard = (options) => api.get(`${BASE}/dashboard`, options);

/** CategoryMetricDTO[] ordenado de mayor a menor. */
export const getTopCategories = (options) => api.get(`${BASE}/top-categories`, options);

/** WeeklyTrendDTO[]: { yearWeek: 'AAAA-WNN', totalReported, totalResolved } */
export const getWeeklyTrends = (options) => api.get(`${BASE}/weekly-trends`, options);
