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
| [docs/](docs/) | Diagramas, evidencias, pruebas, informe de análisis y presentación |

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

1. **Crear la colección en Qdrant** con la dimensión del modelo de embeddings:
   ```bash
   curl -X PUT http://localhost:6333/collections/knowledge_articles \
     -H "Content-Type: application/json" \
     -d '{"vectors": {"size": 1536, "distance": "Cosine"}}'
   ```
2. **Importar los flujos** de `n8n/workflows/` en n8n (están montados en `/workflows`).
3. **Crear las credenciales en n8n** (tipo *Header Auth*):
   - Webhook del flujo 02: nombre `N8N_WEBHOOK_AUTH_HEADER` y valor `N8N_WEBHOOK_AUTH_VALUE` del `.env`.
   - OpenRouter: `Authorization: Bearer <clave>`.
   - Qdrant, si tiene clave.
4. **Activar los flujos** y ejecutar el flujo 01 para indexar los artículos verificados.

> El archivo de Compose no se probó en el equipo donde se escribió (Docker no estaba disponible). La ejecución local con H2 sí está probada.

## Variables de entorno

| Variable | Usada por | Descripción |
|---|---|---|
| `DB_URL`, `DB_DRIVER`, `DB_USERNAME`, `DB_PASSWORD` | backend | Base de datos. Por defecto H2 en memoria |
| `N8N_WEBHOOK_URL` | backend | Webhook del flujo 02 |
| `N8N_WEBHOOK_AUTH_HEADER`, `N8N_WEBHOOK_AUTH_VALUE` | backend | Encabezado de autenticación hacia n8n. Vacío = no se envía |
| `CALLBACK_TOKEN` | backend y n8n | Token `X-Callback-Token` de los callbacks. Vacío = no se exige |
| `CORS_ALLOWED_ORIGINS` | backend | Orígenes permitidos para el frontend |
| `SPRING_BASE_URL`, `QDRANT_URL`, `QDRANT_COLLECTION` | n8n | Servicios que consultan los flujos |
| `OPENROUTER_BASE_URL`, `EMBEDDING_MODEL`, `EMBEDDING_DIM`, `LLM_MODEL` | n8n | Modelos de embeddings y de chat |
| `RAG_SCORE_THRESHOLD`, `RAG_TOP_K`, `MIN_CHARS_DESCRIPCION` | n8n | Umbral de similitud, cantidad de fuentes y largo mínimo de la descripción |

Las claves nunca se escriben en el código ni en el repositorio: van en `.env` o en las credenciales de n8n.

## Pruebas

```bash
cd backend
mvn test
```

24 pruebas de integración con MockMvc sobre H2 y los datos iniciales. Cubren:

- validaciones y la regla de incidentes cerrados;
- filtros de analítica;
- verificación de artículos;
- estados `NO_RESULTS` y `FAILED`;
- una sola calificación por respuesta;
- el filtro del token de callback.

## Equipo y ramas

| Rama | Responsable | Alcance |
|---|---|---|
| `mafe` | Mafeqm | Backend Spring Boot, modelo de datos, datos iniciales |
| `dev-Lisseth` | lissethkatherine | Flujos n8n (indexación y RAG) |
| `devsergy` | velascodazasergio-png | Frontend e integración frontend ↔ backend ↔ n8n |
| `main` | — | Integración final |

El uso de herramientas de IA en el desarrollo está documentado en [AI_USAGE.md](AI_USAGE.md).
