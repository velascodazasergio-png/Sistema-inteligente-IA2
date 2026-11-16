// Analytics service (RF06): indicators computed with queries in the backend.
// Contract: backend/src/main/java/com/mafe/incidentai/controller/AnalyticsController.java
// All accept the optional filters { categoryId, priority, startDate, endDate } (dates as YYYY-MM-DD).

import { api } from '../api.js';
import { startOfDay, endOfDay } from './incidents.service.js';

const BASE = '/api/v1/analytics';

// Turns the form filters into query parameters
const query = (f = {}) => ({
  categoryId: f.categoryId || undefined,
  priority: f.priority || undefined,
  startDate: startOfDay(f.startDate),
  endDate: endOfDay(f.endDate),
});

/** AnalyticsDashboardDTO: totals, resolution %, average hours, AI rating, top categories and weekly trend. */
export const getDashboard = (filters, options) => api.get(`${BASE}/dashboard`, { ...options, query: query(filters) });

/** CategoryMetricDTO[] sorted from highest to lowest. */
export const getTopCategories = (filters, options) => api.get(`${BASE}/top-categories`, { ...options, query: query(filters) });

/** WeeklyTrendDTO[]: { yearWeek: 'AAAA-WNN', totalReported, totalResolved } */
export const getWeeklyTrends = (filters, options) => api.get(`${BASE}/weekly-trends`, { ...options, query: query(filters) });
