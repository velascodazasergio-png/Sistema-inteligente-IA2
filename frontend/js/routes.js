// Mapa de secciones de la aplicación.
// - load: módulo de la vista. Si no existe, se muestra la vista "pendiente",
//   que lista los datos que la sección necesita del backend.
// - needs / available: sirven para coordinar los contratos con backend.
//   Describen datos, no rutas: los endpoints los define quien construye la API.

export const DEFAULT_ROUTE = 'dashboard';

export const routes = [
  {
    path: 'dashboard',
    title: 'Dashboard',
    rf: 'RF07',
    icon: 'i-dashboard',
    description: 'Estado general del departamento de soporte: indicadores y gráficos calculados sobre datos reales.',
    keywords: ['inicio', 'kpi', 'indicadores', 'resumen', 'graficos'],
    needs: [
      'Indicadores: total de incidentes, pendientes, resueltos, porcentaje de resolución y tiempo promedio de atención.',
      'Conteos de incidentes por categoría, por prioridad y por estado.',
      'Evolución semanal y mensual de incidentes.',
      'Distribución de los tiempos de resolución.',
      'Todo lo anterior filtrable por fecha inicial, fecha final, categoría y prioridad.',
    ],
  },
  {
    path: 'incidentes',
    title: 'Incidentes',
    rf: 'RF01',
    icon: 'i-incident',
    description: 'Registro, consulta, filtrado y actualización del estado de los incidentes.',
    keywords: ['tickets', 'registrar', 'crear', 'nuevo'],
    needs: [
      'Listado de incidentes con paginación, filtros y ordenamiento.',
      'Detalle de un incidente.',
      'Registro de incidente: título, descripción, categoría, prioridad y usuario responsable.',
      'Actualización del estado de un incidente.',
      'Catálogos de categorías y de usuarios para los formularios.',
    ],
  },
  {
    path: 'conocimiento',
    title: 'Base de conocimiento',
    rf: 'RF02',
    icon: 'i-book',
    description: 'Artículos técnicos verificados. Solo los activos y verificados se usan como fuentes del asistente.',
    keywords: ['articulos', 'kb', 'soluciones', 'documentos'],
    needs: [
      'Listado y búsqueda de artículos técnicos.',
      'Crear, consultar y editar artículos: título, problema, síntomas, causa y solución.',
      'Activar / desactivar un artículo y marcarlo como verificado.',
    ],
    note: 'La entidad ArticuloConocimiento todavía no existe en el backend.',
  },
  {
    path: 'asistente',
    title: 'Asistente IA',
    rf: 'RF05',
    icon: 'i-sparkles',
    description: 'Orientación técnica generada exclusivamente con los documentos recuperados (RAG).',
    keywords: ['ia', 'rag', 'chat', 'consulta', 'ayuda'],
    available: [
      'POST /api/v1/asistencia-ia/solicitar — existe, pero hoy devuelve una respuesta simulada (esqueleto).',
    ],
    needs: [
      'Obtener la respuesta de una consulta: solución sugerida, pasos recomendados y fuentes.',
      'Similitud numérica de cada fuente recuperada desde Qdrant.',
      'Estado de la consulta: Pendiente, Respondida, Sin resultados o Error.',
    ],
  },
  {
    path: 'historial',
    title: 'Historial de consultas',
    rf: 'RF08',
    icon: 'i-history',
    description: 'Consultas inteligentes realizadas, sus respuestas, fuentes y calificación.',
    keywords: ['consultas', 'calificar', 'evaluacion', 'estrellas'],
    needs: [
      'Listado de consultas: fecha, incidente, pregunta, estado, resumen de respuesta y fuentes.',
      'Historial de asistencias de un incidente concreto.',
      'Registrar una calificación de 1 a 5 con comentario.',
    ],
  },
  {
    path: 'analitica',
    title: 'Analítica',
    rf: 'RF06',
    icon: 'i-chart',
    description: 'Tendencias, distribuciones y tiempos calculados con consultas SQL sobre los incidentes históricos.',
    keywords: ['tendencias', 'estadisticas', 'reportes', 'sql'],
    needs: [
      'Tendencias de incidentes por periodo.',
      'Porcentaje de resolución y tiempos promedio de atención.',
      'Categorías con mayor número de problemas.',
    ],
  },
  {
    path: 'configuracion',
    title: 'Configuración',
    icon: 'i-settings',
    description: 'Conexión con la API de Spring Boot.',
    keywords: ['ajustes', 'api', 'url', 'conexion', 'backend'],
    load: () => import('./views/configuracion.js'),
  },
];
