// Mapa de secciones de la aplicación.
// - load: módulo de la vista. Si no existe, se muestra la vista "pendiente",
//   que lista los datos que la sección necesita del backend.
// - group: sección de la barra lateral. navLabel: nombre corto en la barra (opcional).
// - needs / available: solo para secciones pendientes; sirven para coordinar
//   los contratos con backend.

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
    load: () => import('./views/analitica.js'),
  },
  {
    path: 'configuracion',
    group: 'Sistema',
    title: 'Configuración',
    icon: 'i-settings',
    description: 'Conexión con la API de Spring Boot.',
    keywords: ['ajustes', 'api', 'url', 'conexion', 'backend'],
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
    load: () => import('./views/componentes.js'),
  },
];
