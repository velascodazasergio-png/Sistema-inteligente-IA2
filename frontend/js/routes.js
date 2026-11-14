// Mapa de secciones de la aplicación.
// - load: módulo de la vista. Si no existe, se muestra la vista "pendiente",
//   que lista los datos que la sección necesita del backend.
// - group: sección de la barra lateral. navLabel: nombre corto en la barra (opcional).
// - needs / available: solo para secciones pendientes; sirven para coordinar
//   los contratos con backend.
// - roles: roles que pueden abrir la sección (sin "roles": cualquier usuario con sesión).
//   Es solo la interfaz: el backend vuelve a comprobar cada permiso.
// - byRole: título o descripción distintos según el rol (p. ej. "Mis incidentes").

import { ROLE, currentUser, canAccess } from './auth.js';

const STAFF = [ROLE.ADMIN, ROLE.TECH];

export const DEFAULT_ROUTE = 'dashboard';

export const routes = [
  {
    path: 'dashboard',
    group: 'General',
    title: 'Dashboard',
    rf: 'RF07',
    icon: 'i-dashboard',
    description: 'Estado general del departamento de soporte: indicadores y gráficos calculados sobre datos reales.',
    keywords: ['inicio', 'kpi', 'indicadores', 'resumen', 'graficos'],
    roles: STAFF,
    load: () => import('./views/dashboard.js'),
  },
  {
    path: 'incidentes',
    group: 'Operación',
    title: 'Incidentes',
    rf: 'RF01',
    icon: 'i-incident',
    description: 'Registro, consulta, filtrado y actualización del estado de los incidentes.',
    keywords: ['tickets', 'registrar', 'crear', 'nuevo'],
    byRole: {
      EMPLOYEE: {
        title: 'Mis incidentes',
        description: 'Reporta un problema y sigue el estado de los incidentes que registraste.',
      },
    },
    load: () => import('./views/incidentes.js'),
  },
  {
    path: 'conocimiento',
    group: 'Operación',
    title: 'Base de conocimiento',
    navLabel: 'Conocimiento',
    rf: 'RF02',
    icon: 'i-book',
    description: 'Artículos técnicos verificados. Solo los activos y verificados se usan como fuentes del asistente.',
    keywords: ['articulos', 'kb', 'soluciones', 'documentos'],
    byRole: {
      EMPLOYEE: { description: 'Guías verificadas por el equipo de soporte para resolver problemas frecuentes.' },
    },
    load: () => import('./views/conocimiento.js'),
  },
  {
    path: 'asistente',
    group: 'Inteligencia',
    title: 'Asistente IA',
    rf: 'RF05',
    icon: 'i-sparkles',
    description: 'Orientación técnica generada exclusivamente con los documentos recuperados (RAG).',
    keywords: ['ia', 'rag', 'chat', 'consulta', 'ayuda'],
    byRole: {
      EMPLOYEE: { description: 'Pide orientación sobre uno de tus incidentes, basada en la base de conocimiento verificada.' },
    },
    load: () => import('./views/asistente.js'),
  },
  {
    path: 'historial',
    group: 'Inteligencia',
    title: 'Historial de consultas',
    navLabel: 'Historial',
    rf: 'RF08',
    icon: 'i-history',
    description: 'Consultas inteligentes realizadas, sus respuestas, fuentes y calificación.',
    keywords: ['consultas', 'calificar', 'evaluacion', 'estrellas'],
    byRole: {
      EMPLOYEE: { title: 'Mis consultas', description: 'Consultas a la IA sobre tus incidentes, con sus fuentes y tu calificación.' },
    },
    load: () => import('./views/historial.js'),
  },
  {
    path: 'analitica',
    group: 'Análisis',
    title: 'Analítica',
    rf: 'RF06',
    icon: 'i-chart',
    description: 'Tendencias, distribuciones y tiempos de atención, filtrables por fecha, categoría y prioridad.',
    keywords: ['tendencias', 'estadisticas', 'reportes', 'sql'],
    roles: STAFF,
    load: () => import('./views/analitica.js'),
  },
  {
    path: 'configuracion',
    group: 'Sistema',
    title: 'Configuración',
    icon: 'i-settings',
    description: 'Conexión con la API de Spring Boot.',
    keywords: ['ajustes', 'api', 'url', 'conexion', 'backend'],
    roles: [ROLE.ADMIN],
    load: () => import('./views/configuracion.js'),
  },
  {
    // Catálogo de componentes con datos de demostración. No aparece en la
    // sidebar; se abre en #/componentes o desde el buscador.
    path: 'componentes',
    group: 'Sistema',
    title: 'Componentes',
    icon: 'i-layers',
    description: 'Catálogo de componentes reutilizables (datos de demostración).',
    keywords: ['catalogo', 'demo', 'ui', 'design system'],
    nav: false,
    roles: [ROLE.ADMIN],
    load: () => import('./views/componentes.js'),
  },
  {
    // Pantalla de inicio de sesión: sin barra lateral ni barra superior
    path: 'login',
    title: 'Iniciar sesión',
    icon: 'i-logo',
    nav: false,
    public: true,
    layout: 'auth',
    load: () => import('./views/login.js'),
  },
];

/** La ruta con el título y la descripción que corresponden al rol. */
export function resolveRoute(route, role = currentUser()?.role) {
  const override = route.byRole?.[role];
  return override ? { ...route, ...override, navLabel: override.title ?? route.navLabel } : route;
}

/** Secciones que puede abrir el usuario actual (menú lateral y buscador). */
export function visibleRoutes() {
  const role = currentUser()?.role;
  return routes.filter((route) => !route.public && canAccess(route, role)).map((route) => resolveRoute(route, role));
}
