# Documento de pruebas

Pruebas ejecutadas sobre la rama `devsergy` el 12 de noviembre de 2026. La sección 3b recoge la auditoría con n8n, Qdrant y PostgreSQL reales, que encontró dos defectos críticos que las pruebas anteriores no detectaban.

| Nivel | Herramienta | Casos | Resultado |
|---|---|---:|---|
| Integración del backend | JUnit 5 + MockMvc (`mvn test`) | 27 | ✅ 27 / 27 |
| Unitarias del frontend | `node:test` (`node --test "frontend/tests/*.test.mjs"`) | 23 | ✅ 23 / 23 |
| Contrato de la API | Colección Postman / script HTTP | 25 | ✅ 25 / 25 |
| Contrato del flujo de IA | n8n simulado con el contrato del flujo 02 | 4 | ✅ 4 / 4 |
| Integración real (auditoría) | Docker: n8n, Qdrant, PostgreSQL y OpenRouter reales | 10 escenarios | ✅ 7 · ⏸️ 3 sin clave de OpenRouter |
| RAG e indexación de punta a punta | Docker real + OpenRouter simulado (embeddings y LLM) | 7 | ✅ 7 / 7 |
| Despliegue | Docker Compose (PostgreSQL, backend, frontend, Qdrant, n8n) | 8 | ✅ 8 / 8 |
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
# Tests run: 27, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
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
| 25 | Indexación (RF03) | `statusChangeTriggersReindexWithAuthHeader` | Activar o desactivar un artículo avisa al flujo 01 de n8n tras el commit, con el encabezado de autenticación | ✅ |
| 26 | Indexación (RF03) | `creatingAnUnverifiedArticleDoesNotTriggerIndexing` | Crear un artículo (nace sin verificar) no llama a n8n | ✅ |
| 27 | Indexación (RF03) | `failedChangeDoesNotTriggerIndexing` | Un cambio que falla (404) no llama a n8n | ✅ |

## 1b. Pruebas unitarias del frontend

**Ubicación:** [`frontend/tests/logic.test.mjs`](../../frontend/tests/logic.test.mjs). Usan el ejecutor de pruebas que trae Node 18 o superior, sin dependencias ni navegador.

```bash
node --test "frontend/tests/*.test.mjs"
# tests 23 · pass 23 · fail 0
```

| Grupo | Casos | Qué valida |
|---|---:|---|
| `format.js` | 4 | Semana ISO, porcentajes, conteos, fechas inválidas |
| `analytics-utils.js` | 4 | Pendientes, críticos abiertos y rangos de tiempo con las reglas del backend; periodo anterior; variación sin dividir entre cero |
| `ai-response.js` | 5 | Separación de la respuesta RAG en solución, explicación, pasos y advertencias; pasos sin cortar números ("4740.", "2.3"); resumen por estado |
| `catalogs.js` / `ui.js` | 4 | Estado "Sin resultados" deducido, los 5 estados del backend, escape de HTML ante valores inesperados |
| `api.js` y servicio de IA | 6 | URL y parámetros, cuerpo JSON, mensajes de error de Spring (400 y 409), error de red, validación previa de la descripción |

## 2. Contrato de la API

Las 25 peticiones de [`postman_collection.json`](../evidencias/postman_collection.json) se ejecutaron contra el backend local. Cada una incluye una prueba del código HTTP esperado.

**Resultado:** 25 de 25. El detalle, con un extracto de cada respuesta, está en [`ejecucion-api.md`](../evidencias/ejecucion-api.md).

## 3. Contrato del flujo de IA (n8n simulado)

Se reemplazó n8n por un servidor simulado. Ese servidor exige el encabezado de autenticación y responde al backend con el mismo formato y los mismos *callbacks* que el flujo [`02-asistencia-rag.json`](../../n8n/workflows/02-asistencia-rag.json), incluido el `X-Callback-Token`.

| # | Escenario | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| 1 | n8n devuelve una respuesta con 2 fuentes | `PENDING` → `COMPLETED`; el asistente muestra diagnóstico, solución, pasos, fuentes con similitud 0,912 y 0,7834, y advertencias | ✅ |
| 2 | n8n no encuentra fuentes relevantes | `NO_RESULTS` y aviso de información insuficiente | ✅ |
| 3 | n8n falla al generar la respuesta | `FAILED` con el código `LLM_FALLO` y su mensaje | ✅ |
| 4 | n8n apagado o sin token correcto | `COMMUNICATION_ERROR` con el código `N8N_UNREACHABLE` | ✅ |

> **Límite de esta prueba:** el servidor simulado aceptaba la petición del backend aunque llegara con HTTP/2. El n8n real no la contesta (defecto 10), así que esta prueba no demuestra la integración real. Esa la cubre la sección 3b.

## 3b. Auditoría con los servicios reales

Entorno: `docker compose` con PostgreSQL, el backend del repositorio, n8n 1.80 con los dos flujos importados y activos, y Qdrant 1.12 con la colección `knowledge_articles` de 1536 dimensiones. OpenRouter se configuró con una **clave inválida a propósito**, porque el equipo no tenía clave en el momento de la prueba.

Antes de los escenarios se reinició el backend dos veces, para probar la persistencia en PostgreSQL. Cada cifra de la API se comparó con una consulta SQL en la misma base.

| # | Escenario | Resultado obtenido | Estado |
|---|---|---|---|
| 1 | Incidente de conectividad con soluciones relevantes | La consulta llega a n8n y OpenRouter y queda `FAILED / EMBEDDING_FALLO` en unos 2 s | ⏸️ No verificable sin clave |
| 2 | Error de ejecución de una aplicación | Igual que el 1 | ⏸️ No verificable sin clave |
| 3 | Incidente sin conocimiento suficiente | Igual que el 1. La rama "sin información" existe en el flujo, pero no se ejecutó | ⏸️ No verificable sin clave |
| 4 | Incidentes por categoría frente a SQL | Las 6 categorías y el top de categorías coinciden con `GROUP BY` en PostgreSQL | ✅ |
| 5 | Evolución semanal | Las 9 semanas coinciden con el recálculo SQL por semana ISO | ✅ |
| 6 | Desactivar un artículo | El flujo 01 real borra su vector de Qdrant (`deleted: [10]`) y el artículo sale de los utilizables. El flujo 02 además filtra por activo y verificado y revalida cada fuente en Spring Boot | ✅ |
| 7 | Descripción vacía | 400 en `/incidents` y en `/ai-assistance/request`; no se crea ninguna consulta | ✅ |
| 8 | Fallo en embeddings | OpenRouter responde 401. El flujo 02 reporta el fallo y la consulta queda `FAILED / EMBEDDING_FALLO` en la base. El flujo 01 responde 502 `INDEXACION_FALLO` | ✅ |
| 9 | Historial de un incidente | Devuelve estados, fallos, fuentes y similitud (0,93) | ✅ |
| 10 | Calificar una respuesta | 201 y fila guardada en `ratings`. Una segunda calificación da 409 y un puntaje fuera de rango da 400. El promedio del dashboard (4,6) coincide con `AVG(score)` | ✅ |

Otras comprobaciones:
- **Seguridad:** sin token o con un token incorrecto, los webhooks de n8n responden 403; los callbacks del backend sin token responden 401.
- **Filtros:** los de prioridad, categoría y fechas cambian los resultados y coinciden con SQL (`CRITICAL`: 38 incidentes y 84,21 %; octubre: 83).
- **Datos iniciales:** sin fechas inválidas, sin resueltos sin fecha y sin descripciones repetidas.

**Pendiente:** repetir los escenarios 1, 2 y 3 con la clave de OpenRouter del equipo.

## 3c. RAG e indexación de punta a punta (OpenRouter simulado)

Para ejecutar los caminos que la clave inválida no alcanza, se reemplazó **solo OpenRouter** por un servidor simulado dentro de la red de Docker. Backend, PostgreSQL, n8n con los flujos del repositorio y Qdrant fueron los reales.

El simulador:
- calcula embeddings de 1536 dimensiones por bolsa de palabras, así que textos que comparten términos quedan cerca;
- como LLM, responde con el contenido de la primera `<fuente>` que recibe, en el JSON que espera el flujo 02.

La prueba demuestra el funcionamiento del flujo, **no la calidad de las respuestas del modelo**. El umbral se bajó a 0,25 porque los vectores por bolsa de palabras dan similitudes menores que un modelo real.

| # | Comprobación | Resultado obtenido | Estado |
|---|---|---|---|
| 1 | Indexación completa sin crear la colección a mano | El flujo 01 crea `knowledge_articles` (1536, coseno) y guarda 10 vectores = 10 artículos utilizables | ✅ |
| 2 | Reindexar todo otra vez | Siguen 10 vectores: no hay duplicados (id del punto = id del artículo) | ✅ |
| 3 | Desactivar y reactivar un artículo desde la API | El backend avisa al flujo 01: el vector se borra y se recrea en unos 0,5 s | ✅ |
| 4 | Editar un artículo y volver a verificarlo | Al editar queda sin verificar y su vector se borra; al verificarlo se reindexa con el texto nuevo (cambia `contentHash`) | ✅ |
| 5 | Incidente de VPN con error 809 | `COMPLETED`; fuente: artículo #1 "IPSec VPN Error 809 Troubleshooting Runbook" (similitud 0,56); la solución sale de ese artículo | ✅ |
| 6 | Incidente sin relación (cafetera) | `NO_RESULTS`: "Sin información suficiente…", sin fuentes ni solución | ✅ |
| 7 | Mismo incidente de VPN con el artículo #1 desactivado | `NO_RESULTS`: el artículo ya no se usa como fuente | ✅ |

## 4. Despliegue con Docker Compose

Probado con Docker 29.4 y Compose v5.1. Los puertos del host se reasignaron solo para la prueba, porque el equipo tenía otros proyectos ocupándolos.

| # | Verificación | Resultado |
|---|---|---|
| 1 | Se construye la imagen del backend y arrancan los 5 servicios | ✅ |
| 2 | `data.sql` se carga en PostgreSQL 16 | ✅ |
| 3 | El dashboard en PostgreSQL da los mismos valores que en H2 (86,67 %, 4,74 h, filtro: 8 incidentes y 87,5 %) | ✅ |
| 4 | Registrar un incidente y un artículo en PostgreSQL (los ID continúan en 151 y 11) | ✅ |
| 5 | Al reiniciar el backend los datos se conservan, la carga inicial no se duplica y se pueden seguir creando registros | ✅ (desde `11b3192`; antes las altas daban 500 tras reiniciar, defecto 11) |
| 6 | El frontend (nginx), Qdrant y n8n responden; se crea la colección en Qdrant | ✅ |
| 7 | Los dos flujos se importan y se activan en n8n 1.80 | ✅ |
| 8 | Los healthchecks del backend, Qdrant y n8n pasan a `healthy` y n8n espera a sus dependencias | ✅ |

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
| 9 | Un paso con un número ("evento 4740.") se cortaba en dos | Separación de pasos demasiado amplia | `69a3077` |
| 10 | **Crítico.** Con el n8n real, toda consulta terminaba en `COMMUNICATION_ERROR` a los 15 s | El `HttpClient` del JDK usa HTTP/2 y pide `Upgrade: h2c`; n8n lo trata como un websocket y no responde | `8c054dd` |
| 11 | **Crítico.** Tras reiniciar el backend con PostgreSQL, toda alta daba 500 | `data.sql` reinicia los contadores de id en cada arranque | `11b3192` |
| 12 | PostgreSQL y Qdrant se publicaban en todas las interfaces de red; faltaban healthchecks | Configuración del Compose | `0d56832` |
| 13 | El flujo 02 tenía 2 nodos duplicados (mismo nombre e `id`) | Copia de nodos al armar el flujo | `b101c15` |
| 14 | Un artículo nuevo, editado o desactivado no se reflejaba en Qdrant hasta reindexar a mano | Nadie llamaba al flujo 01 | `8e0eefb` |
| 15 | La colección de Qdrant había que crearla a mano | El flujo 01 suponía que existía | `a036f64` |

## 7. Pendientes

- Probar una consulta real con OpenRouter y Qdrant, con las claves del equipo (escenarios 1, 2 y 3).
- Pruebas de extremo a extremo del frontend en navegador (hoy son capturas revisadas manualmente).
