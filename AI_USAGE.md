# Uso de inteligencia artificial en el proyecto

Este documento registra las herramientas de IA usadas en el desarrollo, su participación, los componentes en los que intervinieron y las decisiones técnicas tomadas con su apoyo, como pide el enunciado.

> El asistente RAG que forma parte del producto (n8n + Qdrant + OpenRouter) es una **funcionalidad del sistema**, no una herramienta de desarrollo. Está documentado en el [README](README.md).

---

## 1. Rama `devsergy` — frontend e integración (velascodazasergio-png)

### Herramienta

| Herramienta | Modelo | Uso |
|---|---|---|
| Claude Code (Anthropic), extensión de VS Code | Claude Opus | Generación y modificación de código, revisión de contratos entre componentes, pruebas y documentación |

### Forma de trabajo

- El integrante definió el alcance de cada etapa con un *prompt* maestro del frontend y con instrucciones puntuales ("parte 2", "parte 3", "haz lo que falta", etc.).
- Claude Code inspeccionó el repositorio y las ramas de las compañeras, propuso el plan y escribió el código.
- Antes de cada *commit* verificó el resultado: compilación, pruebas, capturas de pantalla y llamadas reales a la API.
- Cada *commit* sigue Conventional Commits con descripción en inglés, por indicación del integrante.

### Componentes asistidos

| Componente | Participación de la IA | Commits principales |
|---|---|---|
| Estructura de carpetas del repositorio | Generada a partir de la lista del integrante | `34b89f7`, `f37f6fc` |
| Frontend, Etapa 1: layout, sidebar, router, cliente HTTP, configuración | Código generado | `2cf0948` |
| Frontend, Etapa 2: gráficos SVG, KPI, tabla, modal, estrellas, skeletons | Código generado | `1c202de` |
| Frontend, Etapa 3: Dashboard, Incidentes, Asistente, Historial, Analítica | Código generado | `8581b30` |
| Base de conocimiento (frontend) y adaptación a nuevos endpoints | Código generado | `04e74ec`, `7dfe954` |
| Correcciones en el backend de la rama `mafe`: BOM en `data.sql`, consulta semanal, CORS | Diagnóstico y corrección | `89c1f37`, `7e4960e`, `df54a0a` |
| Endpoints nuevos del backend: catálogos, detalle, artículos, historial global, filtros de analítica, tendencia mensual | Código generado | `cdb337e` … `c916110`, `3d9e99d`, `e5dd773` |
| Integración con los flujos n8n: autenticación del webhook, endpoint de fallos, token de callback | Análisis de los flujos y código | `a4a2a8a` |
| Pruebas de integración del backend (24 casos) | Código generado | `91f530f` |
| Pruebas unitarias del frontend (23 casos) | Código generado | — |
| README, Docker Compose, `.env.example` y este documento | Redacción y prueba del Compose | — |
| Diagramas, informe de análisis, documento de pruebas, evidencias y presentación (`docs/`) | Generados a partir del código y de consultas reales a la base | — |

### Decisiones técnicas tomadas con apoyo de la IA

1. **Frontend sin frameworks**, con módulos ES y un *router* por *hash*. Así funciona en cualquier servidor estático, como exige el enunciado.
2. **Un solo cliente HTTP** (`api.js`) con *timeout*, cancelación al cambiar de sección y traducción de los errores de Spring.
3. **Gráficos propios en SVG** con tabla oculta para lectores de pantalla, en lugar de librerías externas.
4. **No mostrar datos inventados.** Mientras faltaban endpoints, las secciones explicaban qué datos necesitaban. Los datos de demostración solo existen en el catálogo de componentes, marcados como tales.
5. **Indicadores calculados en la base de datos** (JPQL con filtros opcionales). Las distribuciones que el backend no agrega se cuentan en el cliente con las mismas reglas.
6. **Comparación temporal** frente al periodo anterior de igual duración, usando el mismo endpoint de analítica.
7. **Estados de consulta:** se agregó `NO_RESULTS` (n8n responde sin fuentes) y `FAILED` (n8n falla al procesar), para cubrir los estados *Sin resultados* y *Error* del enunciado.
8. **Artículos estructurados** en Problema, Síntomas, Causa y Solución. El backend arma `content` a partir de esas secciones, así el flujo de indexación de n8n sigue funcionando sin cambios.
9. **Editar el título o el contenido de un artículo verificado lo deja sin verificar**, para que la IA solo use contenido revisado.
10. **Seguridad de la integración:**
    - encabezado de autenticación hacia el webhook de n8n;
    - token `X-Callback-Token` en los *callbacks*;
    - comparación del token en tiempo constante;
    - secretos solo en variables de entorno.
11. **Mínimo de 20 caracteres** en la descripción de un incidente, igual que el mínimo que exige el flujo RAG.

### Verificación

- `mvn test`: 24 pruebas de integración en verde. `node --test`: 23 pruebas unitarias del frontend en verde.
- Flujo de IA de punta a punta probado con un n8n simulado que respeta el contrato del flujo 02: respondida, sin resultados, fallo y error de conexión.
- Capturas en navegador sin interfaz, en escritorio y en móvil, de cada módulo con datos reales del backend.
- Los cálculos del cliente se contrastaron con los del SQL: por ejemplo, 86,67 % de resolución y 4,74 h de tiempo promedio.

### Limitaciones conocidas

- Docker Compose se probó con PostgreSQL, Qdrant y n8n reales (arranque, carga de datos, reinicio e importación de flujos), pero no se probó una consulta real a OpenRouter porque requiere una clave propia. El flujo de IA de punta a punta se validó con un n8n simulado que respeta el contrato del flujo 02.
- La autenticación de usuarios no está definida en el proyecto. El frontend no tiene inicio de sesión.

---

## 2. Rama `mafe` — backend (Mafeqm)

**Uso de IA:** ⚠️ *pendiente de confirmar por la integrante* (herramienta, modelo y en qué partes la usó). Si no usó IA, debe indicarlo aquí.

### Trabajo registrado en el repositorio (17 commits, del 10 al 12 de noviembre)

| Componente | Commits |
|---|---|
| Proyecto Spring Boot con JPA, Validation, H2 y OpenAPI | `536c8cd` |
| Entidades JPA y relaciones (incidentes, artículos, consultas IA, respuestas, fuentes, calificaciones) | `7c36beb`, `6de2b4a`, `d4b0225` |
| Repositorios y consultas derivadas | `d5bda62` |
| Contratos DTO para n8n y controlador base en Swagger | `58f5a64` |
| `GlobalExceptionHandler` y excepciones de dominio | `194e821` |
| `IncidentService` con Specifications y validación de estados | `dcc4eb0` |
| `N8nWebhookClient` con timeouts | `19f8def` |
| Persistencia encadenada de respuestas, fuentes y similitud | `4ee4e9e` |
| Endpoints de calificación e historial por incidente | `17b41ba` |
| Consultas analíticas y `AnalyticsService` (RF06) | `6075206`, `369e9ea` |
| `data.sql` con 150 incidentes sintéticos | `08218bb` |
| Catálogos, base de conocimiento, detalle, historial global, filtros (versión propia de cambios de `devsergy`) | `527d742`, `fd396c2`, `bcde55b` |

### Cambios hechos sobre su código desde `devsergy`

Están documentados en la sección 1:
- **Correcciones:** BOM en `data.sql`, consulta semanal y CORS.
- **Ampliaciones:** endpoints, estados `NO_RESULTS` y `FAILED`, secciones de artículos y pruebas.

En el merge final (`e41c35b`) se conservó su consulta JPQL `findActiveAndVerifiedArticles` y sus comentarios.

## 3. Rama `dev-Lisseth` — flujos n8n (lissethkatherine)

**Uso de IA:** ⚠️ *pendiente de confirmar por la integrante* (herramienta, modelo y en qué partes la usó). Si no usó IA, debe indicarlo aquí.

### Trabajo registrado en el repositorio (2 commits, 12 de noviembre)

| Componente | Commit |
|---|---|
| Flujo 01: indexación de artículos verificados en Qdrant con embeddings de OpenRouter (RF03) | `e642290` |
| Flujo 02: asistencia RAG con búsqueda vectorial, revalidación en Spring Boot, generación con LLM y reporte de fallos (RF04, RF05) | `fa8059a` |

**Decisiones de diseño visibles en los flujos:**
- nunca se fabrica una respuesta;
- umbral de similitud configurable;
- cada fuente se revalida en el backend antes de usarse;
- los fallos se clasifican con un código.

### Cambios hechos sobre sus flujos desde `devsergy`

- Encabezado `X-Callback-Token` (`$env.CALLBACK_TOKEN`) en los 2 nodos que llaman a Spring Boot (`7dc1a0e`).
- El nodo "Reportar fallo a Spring Boot (endpoint pendiente)" pasó a llamarse "Reportar fallo a Spring Boot", porque el endpoint ya existe. Se actualizaron sus conexiones y se verificó que el flujo se importa en n8n 1.80.

Ningún otro nodo se modificó.
