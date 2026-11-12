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
    │   ├── analytics.service.js
    │   └── ai-assistance.service.js
    └── views/                  # Una vista por sección
        ├── dashboard.js        # RF07
        ├── incidentes.js       # RF01
        ├── asistente.js        # RF05
        ├── historial.js        # RF08
        ├── analitica.js        # RF06
        ├── pending.js          # Secciones sin endpoints (RF02)
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
| Dashboard (RF07) | 6 KPI del SQL del backend, evolución semanal, ranking de categorías, distribución por estado y prioridad, incidentes recientes | ✅ |
| Incidentes (RF01) | Filtros (categoría, prioridad, estado, usuario, fechas), tabla con búsqueda y orden, registro con validación en tiempo real, detalle y cambio de estado | ✅ |
| Asistente IA (RF05) | Elige un incidente, añade contexto y consulta; espera la respuesta de n8n consultando el historial; muestra diagnóstico, pasos, fuentes con similitud y confianza | ✅ |
| Historial (RF08) | Consultas de un incidente con su respuesta, fuentes y calificación; calificar de 1 a 5 con comentario | ✅ |
| Analítica (RF06) | KPI y gráficos filtrables por fecha, categoría y prioridad; distribución de tiempos de resolución | ✅ |
| Base de conocimiento (RF02) | — | ⏳ Falta el controlador en el backend |

Decisiones tomadas por límites actuales del backend:

- **Categorías y usuarios** de los formularios salen de los incidentes registrados, porque no hay endpoints para esos catálogos.
- **Analítica con filtros**: `/api/v1/analytics/*` no acepta filtros, así que con filtros los indicadores se calculan en el cliente sobre `GET /api/v1/incidents` (ya filtrado por el backend) y con las mismas reglas del backend. Sin filtros, los resultados coinciden con los del SQL.
- **"Sin resultados"**: el enum `QueryStatus` no lo incluye. Se muestra cuando una consulta `COMPLETED` llega sin fuentes.
- **Estado del incidente** se filtra en el cliente: el endpoint no recibe ese parámetro.

## Endpoints usados

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api-docs` | Comprobar que el backend está levantado |
| GET | `/api/v1/incidents` | Listado y filtros (Incidentes, Dashboard, Analítica, Asistente, Historial) |
| POST | `/api/v1/incidents` | Registrar incidente |
| PATCH | `/api/v1/incidents/{id}/status` | Cambiar estado |
| GET | `/api/v1/analytics/dashboard` | KPI, top de categorías y tendencia semanal |
| POST | `/api/v1/ai-assistance/request` | Solicitar análisis a la IA |
| GET | `/api/v1/ai-assistance/incidents/{id}/history` | Historial y espera de la respuesta |
| POST | `/api/v1/ai-assistance/responses/{id}/ratings` | Calificar una respuesta |

`POST /api/v1/ai-assistance/responses` lo usa n8n; el frontend no lo llama.

## Pendientes con backend

1. **Base de conocimiento (RF02)**: la entidad `KnowledgeArticle` existe, pero no tiene controlador REST.
2. **Catálogos**: endpoints para listar categorías y usuarios.
3. **Detalle**: `GET /api/v1/incidents/{id}`.
4. **Historial global**: listado de todas las consultas, no solo por incidente.
5. **Filtros en analítica**: que `/api/v1/analytics/*` acepte fecha, categoría y prioridad para calcularlo todo en SQL.
6. **Estado "Sin resultados"** en `QueryStatus`.
7. **Paginación en el servidor** cuando crezca el volumen de incidentes.
8. **Autenticación**: no hay mecanismo definido. Cuando exista, se añade en `api.js`.
