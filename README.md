# Sistema inteligente de análisis de incidentes tecnológicos y asistencia mediante IA

Plataforma para registrar incidentes de soporte, administrar una base de conocimiento técnica, analizar indicadores con SQL y obtener orientación de un asistente de IA que responde **solo** con artículos verificados (RAG).

## Arquitectura

```
┌────────────┐  REST   ┌──────────────────┐  JDBC  ┌──────────────┐
│  Frontend  │ ──────▶ │  Backend Spring  │ ─────▶ │ PostgreSQL / │
│ HTML/CSS/JS│ ◀────── │  Boot (API REST) │ ◀───── │ H2 (desarr.) │
└────────────┘         └──────────────────┘        └──────────────┘
                          │ webhook   ▲ callback
                          ▼ + token   │ + X-Callback-Token
                       ┌──────────────────┐  embeddings / chat  ┌────────────┐
                       │       n8n        │ ──────────────────▶ │ OpenRouter │
                       │ (flujos 01 y 02) │ ◀────────────────── └────────────┘
                       └──────────────────┘
                          │ búsqueda vectorial
                          ▼
                       ┌──────────────────┐
                       │      Qdrant      │
                       └──────────────────┘
```

- El **frontend** solo habla con la API de Spring Boot. Nunca llama a n8n, Qdrant ni OpenRouter.
- El **backend** registra cada consulta (`PENDING`) y la envía al webhook de n8n. n8n devuelve la respuesta o el fallo por callback.
- **n8n** busca en Qdrant los artículos más similares, vuelve a validar en Spring Boot que sigan activos y verificados, y genera la respuesta con el LLM.

| Carpeta | Contenido |
|---|---|
| [backend/](backend/) | API REST Spring Boot 3.4 (Java 21), JPA, Swagger, pruebas de integración |
| [frontend/](frontend/) | Cliente web en HTML, CSS y JavaScript sin frameworks. Ver [frontend/README.md](frontend/README.md) |
| [n8n/workflows/](n8n/workflows/) | Flujo 01: indexación en Qdrant (RF03). Flujo 02: asistencia RAG (RF04, RF05) |
| [database/data.sql](database/data.sql) | Datos iniciales: 150 incidentes, usuarios, categorías y artículos |
| [docs/diagramas/](docs/diagramas/) | Modelo entidad-relación, arquitectura y secuencia del flujo RAG (Mermaid) |
| [docs/informe-analisis/](docs/informe-analisis/) | Informe de análisis de datos con las consultas SQL, resultados y conclusiones |
| [docs/pruebas/](docs/pruebas/) | Documento de pruebas: integración, API, flujo de IA, Docker e interfaz |
| [docs/evidencias/](docs/evidencias/) | Swagger, OpenAPI, colección Postman, ejecución de la API y capturas |
| [docs/presentacion/](docs/presentacion/) | Presentación final (HTML, se abre con doble clic) |

## Requisitos funcionales

| RF | Funcionalidad | Dónde |
|---|---|---|
| RF01 | Registro y gestión de incidentes | `IncidentController` · `#/incidentes` |
| RF02 | Base de conocimiento (activos y verificados) | `KnowledgeArticleController` · `#/conocimiento` |
| RF03 | Indexación de artículos en Qdrant | Flujo n8n 01 |
| RF04, RF05 | Búsqueda semántica y asistente RAG | Flujo n8n 02 · `AiAssistanceController` · `#/asistente` |
| RF06 | Indicadores calculados con SQL | `AnalyticsController` · `#/analitica` |
| RF07 | Dashboard con filtros | `#/dashboard` |
| RF08 | Historial y calificación de respuestas | `#/historial` |

Estados de una consulta a la IA: **Pendiente** (`PENDING`), **Respondida** (`COMPLETED`), **Sin resultados** (`NO_RESULTS`), **Error** (`COMMUNICATION_ERROR` si n8n no responde, `FAILED` si n8n falla al procesar).

## Ejecución local (desarrollo)

Requisitos: Java 21, Maven 3.9 y Python 3 (u otro servidor estático).

```bash
# 1. Backend en http://localhost:8080 (H2 en memoria con los datos iniciales)
cd backend
mvn spring-boot:run

# 2. Frontend en http://localhost:5500
cd frontend
python -m http.server 5500
```

- Swagger: http://localhost:8080/swagger-ui.html
- Consola H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:incidents_db`, usuario `sa`)

Sin n8n en ejecución, las consultas a la IA quedan en estado *Error de conexión*. El resto del sistema funciona.

## Ejecución con Docker Compose

```bash
cp .env.example .env      # completar las claves y tokens
docker compose up -d --build
```

| Servicio | URL |
|---|---|
| Frontend | http://localhost:5500 |
| Backend / Swagger | http://localhost:8080/swagger-ui.html |
| n8n | http://localhost:5678 |
| Qdrant | http://localhost:6333/dashboard |

Pasos después del primer arranque:

1. **Importar los flujos** de `n8n/workflows/` desde la interfaz de n8n (*Workflows → Import from File*). La CLI (`n8n import:workflow`) espera una lista de flujos, así que con ella hay que envolver cada archivo en `[ … ]`.
2. **Crear las credenciales en n8n** (tipo *Header Auth*):
   - Webhooks de los flujos 01 y 02: nombre `N8N_WEBHOOK_AUTH_HEADER` y valor `N8N_WEBHOOK_AUTH_VALUE` del `.env`.
   - OpenRouter: `Authorization: Bearer <clave>`.
   - Qdrant, si tiene clave.

   En los JSON todas las credenciales tienen el id `REEMPLAZAR`: después de importar, abre cada nodo con credencial (Webhook, embeddings, Qdrant y LLM) y elige la que corresponde.
3. **Activar los flujos** e indexar una vez todos los artículos verificados. La colección de Qdrant se crea sola con la dimensión de `EMBEDDING_DIM`:
   ```bash
   curl -X POST http://localhost:5678/webhook/index-knowledge-base \
     -H "X-N8N-Token: <N8N_WEBHOOK_AUTH_VALUE>" -H "Content-Type: application/json" \
     -d '{"all": true}'
   ```
   Desde ahí el backend mantiene el índice al día: al editar, verificar, activar o desactivar un artículo llama al flujo 01, que indexa ese artículo o borra su vector.

> **Probado** con Docker 29 y Compose v5 ([detalle](docs/pruebas/README.md#3b-auditoría-con-los-servicios-reales)):
> - los 5 servicios arrancan, y el backend, Qdrant y n8n pasan sus healthchecks;
> - el backend carga `data.sql` en PostgreSQL y el dashboard coincide con las consultas SQL;
> - tras dos reinicios del backend los datos se conservan y se pueden seguir creando registros;
> - el backend entrega las consultas al n8n real, que llama a OpenRouter y devuelve el resultado o el fallo al backend;
> - el flujo 01 borra de Qdrant los vectores de artículos desactivados.
>
> No se probó una respuesta real del modelo: requiere la clave de OpenRouter del equipo.
>
> Los puertos se publican solo en `127.0.0.1`. Si 5432, 5500, 5678, 6333 u 8080 ya están ocupados en tu equipo, cámbialos en `.env` (`POSTGRES_PORT`, `FRONTEND_PORT`, `N8N_PORT`, `QDRANT_PORT`, `BACKEND_PORT`).

## Variables de entorno

| Variable | Usada por | Descripción |
|---|---|---|
| `DB_URL`, `DB_DRIVER`, `DB_USERNAME`, `DB_PASSWORD` | backend | Base de datos. Por defecto H2 en memoria |
| `N8N_WEBHOOK_URL` | backend | Webhook del flujo 02 |
| `N8N_INDEX_WEBHOOK_URL` | backend | Webhook del flujo 01 para reindexar un artículo cuando cambia. Vacío = desactivado |
| `N8N_WEBHOOK_AUTH_HEADER`, `N8N_WEBHOOK_AUTH_VALUE` | backend | Encabezado de autenticación hacia n8n. Vacío = no se envía |
| `CALLBACK_TOKEN` | backend y n8n | Token `X-Callback-Token` de los callbacks. Vacío = no se exige |
| `CORS_ALLOWED_ORIGINS` | backend | Orígenes permitidos para el frontend |
| `SPRING_BASE_URL`, `QDRANT_URL`, `QDRANT_COLLECTION` | n8n | Servicios que consultan los flujos |
| `OPENROUTER_BASE_URL`, `EMBEDDING_MODEL`, `EMBEDDING_DIM`, `LLM_MODEL` | n8n | Modelos de embeddings y de chat |
| `RAG_SCORE_THRESHOLD`, `RAG_TOP_K`, `MIN_CHARS_DESCRIPCION` | n8n | Umbral de similitud, cantidad de fuentes y largo mínimo de la descripción |

Las claves nunca se escriben en el código ni en el repositorio: van en `.env` o en las credenciales de n8n.

## Pruebas

```bash
cd backend && mvn test                      # backend: 27 pruebas de integración
node --test "frontend/tests/*.test.mjs"     # frontend: 23 pruebas unitarias (Node 18+)
```

Las 27 pruebas de integración del backend con MockMvc sobre H2 y los datos iniciales. Cubren:

- validaciones y la regla de incidentes cerrados;
- filtros de analítica;
- verificación de artículos;
- estados `NO_RESULTS` y `FAILED`;
- una sola calificación por respuesta;
- el filtro del token de callback;
- el aviso de reindexación a n8n solo después del commit.

## Equipo y ramas

| Rama | Responsable | Alcance |
|---|---|---|
| `mafe` | Mafeqm | Backend Spring Boot, modelo de datos, datos iniciales |
| `dev-Lisseth` | lissethkatherine | Flujos n8n (indexación y RAG) |
| `devsergy` | velascodazasergio-png | Frontend e integración frontend ↔ backend ↔ n8n |
| `main` | — | Integración final |

El uso de herramientas de IA en el desarrollo está documentado en [AI_USAGE.md](AI_USAGE.md).
