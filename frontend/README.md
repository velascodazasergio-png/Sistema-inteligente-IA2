# Frontend — Incidentes IA

Cliente web del Sistema inteligente de análisis de incidentes tecnológicos.
Hecho solo con **HTML5, CSS3 y JavaScript (ES modules)**: sin frameworks, sin librerías y sin paso de compilación.

## Cómo ejecutarlo

Los módulos de JavaScript no cargan si abres `index.html` con doble clic (`file://`). Hace falta servirlo con cualquier servidor estático:

```bash
# Opción 1: extensión "Live Server" de VS Code → clic derecho en index.html → Open with Live Server
# Opción 2: Python
cd frontend
python -m http.server 5500
# Abrir http://localhost:5500
```

El backend se espera en `http://localhost:8080`. La URL se puede cambiar desde **Configuración** dentro de la app.

## Estructura

```
frontend/
├── index.html                  # Layout: sidebar, topbar, sprite de iconos SVG
├── assets/
│   ├── icons/favicon.svg
│   └── images/
├── css/
│   ├── variables.css           # Design tokens (colores, tipografía, espaciado, movimiento)
│   ├── base.css                # Reset, foco, accesibilidad, prefers-reduced-motion
│   ├── layout.css              # Sidebar, topbar, vista principal
│   ├── components.css          # Botones, tarjetas, buscador, estados vacíos, formularios, toasts
│   ├── widgets.css             # KPI, gráficos, tabla, modal, estrellas, etiquetas, skeletons
│   ├── modules.css             # Estilos de los módulos (Etapa 3)
│   └── responsive.css          # Tablet y móvil
└── js/
    ├── app.js                  # Punto de entrada
    ├── config.js               # URL base de la API
    ├── api.js                  # Cliente HTTP único (errores, timeouts, cancelación)
    ├── routes.js               # Secciones y datos que necesita cada una
    ├── router.js               # Navegación por hash (#/seccion)
    ├── search.js               # Buscador de secciones (Ctrl+K)
    ├── ui.js                   # Iconos, escape de HTML, toasts
    ├── catalogs.js             # Enums del backend → etiqueta y color
    ├── format.js               # Fechas, porcentajes y conteos
    ├── analytics-utils.js      # Distribuciones, periodos y variaciones (Dashboard y Analítica)
    ├── components/             # Componentes reutilizables (Etapa 2)
    │   ├── charts.js           # barChart, columnChart, lineChart, donutChart (SVG)
    │   ├── kpi-card.js         # kpiCard, animateCounters, formatDuration
    │   ├── data-table.js       # createDataTable: búsqueda, orden, paginación
    │   ├── modal.js            # openModal, confirmDialog (<dialog> nativo)
    │   ├── rating.js           # createStarRating, starsHtml
    │   ├── skeleton.js         # skeletonKpis, skeletonChart, skeletonLines
    │   ├── states.js           # errorState, emptyHtml
    │   └── ai-response.js      # Respuesta de la IA: pasos, fuentes, similitud, calificación
    ├── services/               # Un archivo por recurso del backend
    │   ├── incidents.service.js
    │   ├── catalogs.service.js
    │   ├── knowledge.service.js
    │   ├── analytics.service.js
    │   └── ai-assistance.service.js
    └── views/                  # Una vista por sección
        ├── dashboard.js        # RF07
        ├── incidentes.js       # RF01
        ├── conocimiento.js     # RF02
        ├── asistente.js        # RF05
        ├── historial.js        # RF08
        ├── analitica.js        # RF06
        ├── pending.js          # Sección sin endpoints (respaldo)
        ├── configuracion.js
        └── componentes.js      # Catálogo con datos de demostración
```

Respecto a la estructura de referencia del enunciado, las vistas van en `js/views/` y las llamadas a la API en `js/services/`. Así la lógica de pantalla queda separada del acceso a datos.

## Estado — Etapa 1: base visual y arquitectura

| Elemento | Estado |
|---|---|
| Identidad visual y design tokens | ✅ |
| Sidebar contraíble con tooltips y panel móvil | ✅ |
| Topbar: título, buscador de secciones, estado de la API, acción rápida | ✅ |
| Router sin recargas y foco accesible al navegar | ✅ |
| Cliente de API centralizado | ✅ |
| Configuración de la URL de la API | ✅ |

Las secciones sin endpoints **no muestran datos de ejemplo**. Indican qué datos necesitan del backend para que se puedan acordar los contratos.

## Estado — Etapa 2: componentes reutilizables

| Componente | Qué hace |
|---|---|
| Tarjeta KPI | Valor real, icono, tendencia frente al periodo anterior, barra de progreso, conteo animado |
| Gráficos SVG | Barras horizontales, columnas, líneas y dona, con tooltips, leyendas, animación, estado vacío, redibujado al cambiar de tamaño y `update(data)` para filtros |
| Tabla de datos | Búsqueda instantánea (ignora tildes), ordenamiento, paginación, skeleton de carga, filas clicables con teclado |
| Modal | `<dialog>` nativo con animación; `confirmDialog` devuelve `true` o `false` |
| Calificación | 1 a 5 estrellas con radios nativos (mouse y flechas del teclado) |
| Etiquetas | Colores para los enums `EstadoIncidente`, `Prioridad` y `EstadoConsulta` |
| Skeletons | Placeholders de carga para KPI, gráficos y texto |

**Catálogo:** abre `#/componentes` (o búscalo con Ctrl+K). Usa **datos de demostración**, marcados en la página, y no aparece en la sidebar. Ningún módulo real importa esos datos.

Cada gráfico recibe el `signal` que el router entrega a la vista, para liberar recursos al cambiar de sección.

Los gráficos incluyen una tabla oculta con los mismos datos para lectores de pantalla.

## Estado — Etapa 3: módulos conectados a la API

| Módulo | Qué hace | Estado |
|---|---|---|
| Dashboard (RF07) | Filtros (periodo rápido, fechas, categoría, prioridad); 6 KPI con comparación frente al periodo anterior de igual duración; evolución semanal y mensual, tiempos de resolución, categorías, estado y prioridad; incidentes recientes. Enlace directo `#/dashboard?periodo=30` | ✅ |
| Incidentes (RF01) | Filtros en el servidor, tabla con búsqueda, orden, paginación y columna de acciones (detalle, consultar IA, historial); registro con validación en tiempo real; detalle y cambio de estado; confirmación al cerrar un incidente y al descartar un formulario | ✅ |
| Base de conocimiento (RF02) | Vista de tarjetas o tabla; secciones Problema, Síntomas, Causa y Solución; colores para activo y verificado, pendiente de verificación e inactivo; búsqueda y filtros; crear, editar, activar o desactivar y verificar | ✅ |
| Asistente IA (RF05) | Conversación por incidente; animación de búsqueda mientras n8n procesa; respuesta por secciones (diagnóstico, solución sugerida, pasos, fuentes desplegables con similitud real, advertencias); avisos de sin resultados y de error | ✅ |
| Historial (RF08) | Todas las consultas con filtros por estado e incidente, resumen de la respuesta, estadísticas y detalle; calificar de 1 a 5 con comentario | ✅ |
| Analítica (RF06) | KPI, ranking y tendencia calculados en la base de datos con filtros de fecha, categoría y prioridad; distribución de estados, prioridades y tiempos | ✅ |

Notas:

- **Estados de consulta**: `PENDING` Pendiente, `COMPLETED` Respondida, `NO_RESULTS` Sin resultados, `COMMUNICATION_ERROR` Error de conexión (n8n no respondió) y `FAILED` Error en la IA (n8n falló al procesar, con el motivo).
- **Formato de la respuesta**: el flujo RAG envía `Solución sugerida: …` / `Explicación del problema: …` y los pasos con `Advertencias:`; `ai-response.js` los separa en secciones. Si el texto no sigue ese formato se muestra completo.
- **Distribuciones de Analítica** (estado, prioridad, tiempos) se cuentan en el cliente sobre `GET /api/v1/incidents` con los mismos filtros; los demás indicadores salen del SQL.
- **Artículos**: el backend arma `content` (lo que indexa n8n) a partir de Problema, Síntomas, Causa y Solución. Los artículos antiguos solo tienen `content`; al editarlos se cargan en Problema para reorganizarlos.
- **Editar un artículo verificado** cambiando su título o contenido lo deja sin verificar (regla del backend).

## Diseño visual

- **Tipografía:** Inter para la interfaz y JetBrains Mono para cifras y códigos (Google Fonts). Sin internet se usan las fuentes del sistema.
- **Sidebar** agrupada en General, Operación, Inteligencia, Análisis y Sistema, con el estado de la API al pie.
- **Encabezado de cada sección** con icono, requisito (RF) y ruta.
- **Dashboard:**
  - un bloque de resumen con frases calculadas de los datos reales: pendientes, críticos, categoría más frecuente y comparación con el periodo anterior;
  - un indicador circular del porcentaje de resolución y accesos rápidos;
  - minigráficos semanales en las tarjetas de indicadores.
- **Tablas** con avatares de iniciales y una barra de acento en la fila bajo el cursor. En Incidentes, título y categoría comparten celda para que todas las columnas entren en pantalla.
- **Botón principal** con degradado y brillo al pasar el cursor; tarjetas con filo de luz; fondo con una retícula tenue.

Los degradados de los botones usan tonos oscuros para que el texto blanco cumpla el contraste AA. Las animaciones se desactivan con `prefers-reduced-motion`.

## Pruebas

```bash
# Desde la raíz del repositorio (Node 18 o superior, sin dependencias)
node --test "frontend/tests/*.test.mjs"
```

23 pruebas unitarias de la lógica: cálculos de analítica, formato, interpretación de las respuestas de la IA, catálogos y el cliente HTTP (con `fetch` simulado).

## Endpoints usados

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api-docs` | Comprobar que el backend está levantado |
| GET | `/api/v1/categories`, `/api/v1/users` | Catálogos de formularios y filtros |
| GET | `/api/v1/incidents` | Listado con filtros `categoryId`, `priority`, `status`, `userId`, `startDate`, `endDate` |
| GET | `/api/v1/incidents/{id}` | Detalle (enlaces `#/incidentes?id=…`) |
| POST | `/api/v1/incidents` | Registrar incidente |
| PATCH | `/api/v1/incidents/{id}/status` | Cambiar estado |
| GET | `/api/v1/knowledge-articles` | Buscar artículos: `q`, `categoryId`, `active`, `verified` |
| POST / PUT | `/api/v1/knowledge-articles[/{id}]` | Crear y editar artículos |
| PATCH | `/api/v1/knowledge-articles/{id}/status` | Activar, desactivar o verificar |
| GET | `/api/v1/analytics/dashboard` | KPI, top de categorías y tendencias semanal y mensual, con filtros (también para el periodo anterior) |
| POST | `/api/v1/ai-assistance/request` | Solicitar análisis a la IA |
| GET | `/api/v1/ai-assistance/history` | Historial global (filtro `status`) |
| GET | `/api/v1/ai-assistance/incidents/{id}/history` | Espera de la respuesta en el asistente |
| POST | `/api/v1/ai-assistance/responses/{id}/ratings` | Calificar una respuesta |

`POST /api/v1/ai-assistance/responses` y `POST /api/v1/ai-assistance/queries/{id}/failure` los usa n8n; el frontend no los llama.

## Pendientes

1. **n8n**: sin n8n en ejecución, toda consulta nueva queda en *Error de conexión*. La configuración completa está en el [README del proyecto](../README.md).
2. **Paginación en el servidor** cuando crezca el volumen de incidentes.
3. **Autenticación**: no hay mecanismo definido. Cuando exista, se añade en `api.js`.
