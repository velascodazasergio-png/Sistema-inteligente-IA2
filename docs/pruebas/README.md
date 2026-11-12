# Documento de pruebas

Pruebas ejecutadas sobre la rama `devsergy` el 12 de noviembre de 2026.

| Nivel | Herramienta | Casos | Resultado |
|---|---|---:|---|
| Integración del backend | JUnit 5 + MockMvc (`mvn test`) | 24 | ✅ 24 / 24 |
| Contrato de la API | Colección Postman / script HTTP | 25 | ✅ 25 / 25 |
| Flujo de IA de punta a punta | n8n simulado con el contrato del flujo 02 | 4 | ✅ 4 / 4 |
| Despliegue | Docker Compose (PostgreSQL, backend, frontend, Qdrant, n8n) | 7 | ✅ 7 / 7 |
| Interfaz | Navegador sin interfaz (Edge), escritorio y móvil | 8 vistas | ✅ revisadas |

## 1. Pruebas de integración del backend

**Ubicación:** [`backend/src/test/java/com/mafe/incidentai`](../../backend/src/test/java/com/mafe/incidentai).

**Entorno:**
- Cada clase usa su propia base H2 en memoria, cargada con `data.sql`.
- Cada prueba se revierte al terminar, para que no se afecten entre sí.
- n8n apunta a un puerto cerrado, para simular que no está disponible.

```bash
cd backend
mvn test
# Tests run: 24, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

| # | Grupo | Caso | Qué valida | Resultado |
|---|---|---|---|---|
| 1 | Incidentes | `listsSeedIncidents` | Devuelve los 150 incidentes iniciales | ✅ |
| 2 | Incidentes | `filtersByStatus` | `?status=OPEN` solo devuelve abiertos | ✅ |
| 3 | Incidentes | `rejectsUnknownEnumValueWith400` | Un valor de prioridad inexistente da 400, no 500 | ✅ |
| 4 | Incidentes | `validatesNewIncident` | Título, descripción y categoría inválidos dan 400 con detalle por campo | ✅ |
| 5 | Incidentes | `createsIncidentAsOpen` | Un incidente nuevo nace en `OPEN` | ✅ |
| 6 | Incidentes | `closedIncidentCannotChangeStatus` | Un incidente cerrado no cambia de estado (409) | ✅ |
| 7 | Incidentes | `returnsNotFoundForMissingIncident` | Un ID inexistente da 404 | ✅ |
| 8 | Analítica | `dashboardMatchesSeedData` | 150 incidentes, 130 resueltos, 86,67 % y 3 meses de tendencia | ✅ |
| 9 | Analítica | `dashboardAppliesFilters` | Los filtros de categoría y prioridad se aplican en SQL | ✅ |
| 10 | Analítica | `rejectsInvertedDateRange` | Fecha inicial posterior a la final da 409 | ✅ |
| 11 | Conocimiento | `composesContentFromSectionsAndStartsUnverified` | Arma `content` desde las secciones; el artículo nace sin verificar | ✅ |
| 12 | Conocimiento | `editingContentResetsVerification` | Editar un artículo verificado lo deja sin verificar | ✅ |
| 13 | Conocimiento | `searchesOnlyUsableArticles` | El filtro activo + verificado solo devuelve artículos utilizables | ✅ |
| 14 | IA | `marksCommunicationErrorWhenN8nIsDown` | Sin n8n la consulta queda `COMMUNICATION_ERROR` con el código `N8N_UNREACHABLE` | ✅ |
| 15 | IA | `rejectsShortDescription` | Una descripción de menos de 20 caracteres da 400 | ✅ |
| 16 | IA | `responseWithoutSourcesIsNoResults` | Una respuesta sin fuentes queda `NO_RESULTS` | ✅ |
| 17 | IA | `responseWithSourcesIsCompletedAndCanBeRatedOnce` | Con fuentes queda `COMPLETED`; la segunda calificación da 409 | ✅ |
| 18 | IA | `failureReportMarksQueryAsFailed` | El reporte de fallo de n8n deja la consulta en `FAILED` con su mensaje | ✅ |
| 19 | IA | `failureCannotOverrideAResponse` | Un fallo tardío no pisa una respuesta ya registrada (409) | ✅ |
| 20 | IA | `closedIncidentCannotBeAnalyzed` | No se consulta a la IA sobre un incidente cerrado (409) | ✅ |
| 21 | Seguridad | `rejectsCallbackWithoutToken` | Sin `X-Callback-Token` los callbacks dan 401 | ✅ |
| 22 | Seguridad | `rejectsCallbackWithWrongToken` | Con un token incorrecto dan 401 | ✅ |
| 23 | Seguridad | `acceptsCallbackWithToken` | Con el token correcto pasan el filtro | ✅ |
| 24 | Seguridad | `doesNotProtectOtherEndpoints` | El resto de la API no exige el token | ✅ |

## 2. Contrato de la API

Las 25 peticiones de [`postman_collection.json`](../evidencias/postman_collection.json) se ejecutaron contra el backend local. Cada una incluye una prueba del código HTTP esperado.

**Resultado:** 25 de 25. El detalle, con un extracto de cada respuesta, está en [`ejecucion-api.md`](../evidencias/ejecucion-api.md).

## 3. Flujo de IA de punta a punta

Se reemplazó n8n por un servidor simulado. Ese servidor exige el encabezado de autenticación y responde al backend con el mismo formato y los mismos *callbacks* que el flujo [`02-asistencia-rag.json`](../../n8n/workflows/02-asistencia-rag.json), incluido el `X-Callback-Token`.

| # | Escenario | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| 1 | n8n devuelve una respuesta con 2 fuentes | `PENDING` → `COMPLETED`; el asistente muestra diagnóstico, solución, pasos, fuentes con similitud 0,912 y 0,7834, y advertencias | ✅ |
| 2 | n8n no encuentra fuentes relevantes | `NO_RESULTS` y aviso de información insuficiente | ✅ |
| 3 | n8n falla al generar la respuesta | `FAILED` con el código `LLM_FALLO` y su mensaje | ✅ |
| 4 | n8n apagado o sin token correcto | `COMMUNICATION_ERROR` con el código `N8N_UNREACHABLE` | ✅ |

## 4. Despliegue con Docker Compose

Probado con Docker 29.4 y Compose v5.1. Los puertos del host se reasignaron solo para la prueba, porque el equipo tenía otros proyectos ocupándolos.

| # | Verificación | Resultado |
|---|---|---|
| 1 | Se construye la imagen del backend y arrancan los 5 servicios | ✅ |
| 2 | `data.sql` se carga en PostgreSQL 16 | ✅ |
| 3 | El dashboard en PostgreSQL da los mismos valores que en H2 (86,67 %, 4,74 h, filtro: 8 incidentes y 87,5 %) | ✅ |
| 4 | Registrar un incidente y un artículo en PostgreSQL (los ID continúan en 151 y 11) | ✅ |
| 5 | Al reiniciar el backend los datos se conservan y la carga inicial no se duplica | ✅ |
| 6 | El frontend (nginx), Qdrant y n8n responden; se crea la colección en Qdrant | ✅ |
| 7 | Los dos flujos se importan en n8n 1.80 | ✅ |

No se probó una llamada real a OpenRouter, porque requiere una clave propia.

## 5. Pruebas de interfaz

Capturas en [`docs/evidencias/frontend`](../evidencias/frontend). Se revisó cada módulo con datos reales del backend:

| Vista | Qué se comprobó |
|---|---|
| Dashboard | 6 indicadores, comparación con el periodo anterior (`?periodo=30`), 6 gráficos, recientes |
| Incidentes | Filtros, tabla con columna de acciones sin desbordar, detalle |
| Base de conocimiento | Tarjetas con colores por estado, leyenda, estadísticas |
| Asistente | Conversación, respuesta por secciones, fuentes desplegables, calificación |
| Historial | Filtros, estadísticas, resumen de la respuesta, calificación |
| Analítica | Filtros y gráficos |
| Móvil (500 px) | Menú lateral oculto, KPI en dos columnas, gráficos adaptados |
| Configuración | URL de la API y prueba de conexión |

Accesibilidad revisada en el código:
- foco visible y navegación con teclado (tabla, estrellas, modales);
- etiquetas en todos los campos y `aria-live` en los mensajes;
- tablas ocultas con los datos de cada gráfico para lectores de pantalla;
- respeto de `prefers-reduced-motion`.

## 6. Defectos encontrados y corregidos

| # | Defecto | Causa | Corrección |
|---|---|---|---|
| 1 | El backend no arrancaba | `data.sql` guardado con BOM | `89c1f37` |
| 2 | `/analytics/dashboard` daba 500 | `GROUP BY` inválido en la consulta semanal | `7e4960e` |
| 3 | El navegador bloqueaba la API | Faltaba CORS | `df54a0a` |
| 4 | Parámetros inválidos daban 500 | No había manejador para errores de conversión | `1f00e66` |
| 5 | Dos gráficos de Analítica no se dibujaban | `id` duplicado entre un filtro y un gráfico | `8581b30` |
| 6 | La columna de acciones quedaba fuera de pantalla | Tabla más ancha que la vista | `7dfe954` |
| 7 | Una descripción de 15 a 19 caracteres pasaba el backend y fallaba en n8n | Mínimos distintos (15 frente a 20) | `a4a2a8a` |
| 8 | Las consultas quedaban "Pendientes" para siempre si n8n fallaba | No existía el endpoint de fallos | `a4a2a8a` |
| 9 | Un paso con un número ("evento 4740.") se cortaba en dos | Separación de pasos demasiado amplia | este commit |

## 7. Pendientes

- Probar una consulta real con OpenRouter y Qdrant, con las claves del equipo.
- Pruebas automáticas del frontend (hoy son manuales y visuales).
