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
│   └── responsive.css          # Tablet y móvil
└── js/
    ├── app.js                  # Punto de entrada
    ├── config.js               # URL base de la API
    ├── api.js                  # Cliente HTTP único (errores, timeouts, cancelación)
    ├── routes.js               # Secciones y datos que necesita cada una
    ├── router.js               # Navegación por hash (#/seccion)
    ├── search.js               # Buscador de secciones (Ctrl+K)
    ├── ui.js                   # Iconos, escape de HTML, toasts
    ├── services/               # Un archivo por recurso del backend
    │   └── asistencia.service.js
    └── views/                  # Una vista por sección
        ├── pending.js
        └── configuracion.js
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
| Dashboard, Incidentes, Conocimiento, Asistente, Historial, Analítica | ⏳ Esperan endpoints del backend |

Las secciones pendientes **no muestran datos de ejemplo**. Indican qué datos necesitan del backend para que se puedan acordar los contratos.

## Endpoints usados

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api-docs` | Solo para comprobar que el backend está levantado |
| POST | `/api/v1/asistencia-ia/solicitar` | Preparado en `asistencia.service.js` (el backend aún lo simula) |

## Pendientes con backend

1. **CORS**: el backend debe permitir el origen del frontend (por ejemplo `http://localhost:5500`). Sin esto, el navegador bloquea las respuestas.
2. **Endpoints de lectura** para incidentes, categorías, usuarios, dashboard y analítica.
3. **ArticuloConocimiento**: la entidad aún no existe (RF02).
4. **Similitud por fuente**: `RespuestaIADTO.FuenteDTO` no incluye el valor de similitud que exige RF04/RF05.
5. **Estados de consulta**: el enunciado pide *Pendiente, Respondida, Sin resultados, Error*; el enum actual es `PENDIENTE, COMPLETADA, ERROR_COMUNICACION`.
6. **Autenticación**: no hay mecanismo definido. Cuando exista, se añade en `api.js`.
