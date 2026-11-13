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
   - Qdrant, si tiene clave.

   En los JSON todas las credenciales tienen el id `REEMPLAZAR`: después de importar, abre cada nodo con credencial (Webhook y Qdrant) y elige la que corresponde. La clave de OpenRouter **no** va en n8n: se lee de `OPENROUTER_API_KEY` en el `.env`.
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
| `OPENROUTER_API_KEY` | n8n | Clave de OpenRouter (solo la clave, sin `Bearer`). Tras cambiarla: `docker compose up -d n8n` |
| `OPENROUTER_BASE_URL`, `EMBEDDING_MODEL`, `EMBEDDING_DIM`, `LLM_MODEL` | n8n | Modelos de embeddings y de chat |
| `RAG_SCORE_THRESHOLD`, `RAG_TOP_K`, `MIN_CHARS_DESCRIPCION` | n8n | Umbral de similitud, cantidad de fuentes y largo mínimo de la descripción |

Las claves nunca se escriben en el código ni en el repositorio: van en `.env` o en las credenciales de n8n.

## Pruebas

```bash
cd backend && mvn test                      # backend: 27 pruebas de integración
node --test "frontend/tests/*.test.mjs"     # frontend: 23 pruebas unitarias (Node 22+; con Node 18–20: node --test frontend/tests/logic.test.mjs)
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

---

## Guía completa: instalación, pruebas y despliegue en Ubuntu

Esta guía lleva el proyecto desde cero hasta funcionando en otro equipo con **Ubuntu 22.04 o 24.04**. Todo corre en Docker, así que no hace falta instalar Java, Maven, Node ni PostgreSQL.

### 1. Contexto general

El sistema centraliza los incidentes de un área de soporte tecnológico y aprovecha su historial:

- **Gestión:** registro, filtrado y cambio de estado de incidentes, y una base de conocimiento con artículos técnicos (problema, síntomas, causa y solución).
- **Análisis de datos:** indicadores calculados con SQL sobre PostgreSQL (tendencias, distribuciones, tasa de resolución, tiempos de atención), visibles en un dashboard con filtros.
- **Asistencia con IA (RAG):** ante un incidente, n8n busca en Qdrant los artículos más parecidos y un modelo de OpenRouter redacta una orientación usando **solo** esos artículos. Si no hay artículos relevantes, lo dice en lugar de inventar.

Cada consulta a la IA queda guardada con su respuesta, sus fuentes, su estado y la calificación del usuario.

Los cinco servicios que levanta `docker compose`:

| Servicio | Imagen | Función | Puerto en el equipo |
|---|---|---|---|
| `postgres` | postgres:16.4-alpine | Base de datos relacional | 5432 |
| `backend` | se construye desde `backend/` | API REST Spring Boot | 8080 |
| `frontend` | nginx:1.27-alpine | Sirve la página web | 5500 |
| `qdrant` | qdrant/qdrant:v1.12.4 | Base de datos vectorial | 6333 |
| `n8n` | n8nio/n8n:1.80.0 | Flujos de indexación y RAG | 5678 |

Por seguridad, los puertos se publican **solo en `127.0.0.1`**. Desde otro equipo se accede con un túnel SSH (paso 8).

### 2. Estructura del repositorio

```
.
├── docker-compose.yml          # Los 5 servicios, healthchecks y volúmenes
├── .env.example                # Plantilla de configuración (copiar a .env)
├── backend/                    # API Spring Boot 3.4 · Java 21
│   ├── Dockerfile              # Compilación con Maven y ejecución con JRE 21
│   └── src/
│       ├── main/java/com/mafe/incidentai/
│       │   ├── controller/     # Endpoints REST (incidentes, conocimiento, IA, analítica, catálogos)
│       │   ├── service/        # Reglas de negocio
│       │   ├── repository/     # JPA y consultas analíticas JPQL
│       │   ├── domain/         # Entidades y enumeraciones
│       │   ├── dto/            # Contratos de entrada y salida
│       │   ├── client/         # Llamadas a los webhooks de n8n
│       │   └── config/         # CORS, token de callbacks, cliente HTTP, @Async
│       ├── main/resources/     # application.properties, data.sql, sequences-postgres.sql
│       └── test/               # 27 pruebas de integración
├── frontend/                   # HTML, CSS y JavaScript sin frameworks
│   ├── index.html
│   ├── css/  js/  assets/
│   └── tests/                  # 23 pruebas unitarias (node:test)
├── n8n/workflows/              # 01-indexacion-base-conocimiento.json · 02-asistencia-rag.json
├── database/data.sql           # Copia de los datos iniciales
├── docs/                       # Diagramas, informe de análisis, pruebas, evidencias y presentación
└── AI_USAGE.md                 # Uso de IA durante el desarrollo
```

### 3. Instalar Docker en Ubuntu

```bash
# Paquetes base
sudo apt update
sudo apt install -y ca-certificates curl git openssl

# Repositorio oficial de Docker
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Docker Engine y el plugin Compose
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Usar Docker sin sudo (cierra sesión y vuelve a entrar después)
sudo usermod -aG docker $USER
```

Para comprobarlo, ejecuta `docker --version` y `docker compose version`; los dos deben responder sin error.

### 4. Descargar y configurar el proyecto

```bash
git clone https://github.com/velascodazasergio-png/Sistema-inteligente-de-an-lisis-de-incidentes-tecnol-gicos-y-asistencia-mediante--IA.git incidentes-ia
cd incidentes-ia
cp .env.example .env
```

Genera contraseñas y tokens aleatorios en el `.env`:

```bash
sed -i "s/^POSTGRES_PASSWORD=.*/POSTGRES_PASSWORD=$(openssl rand -hex 24)/" .env
sed -i "s/^N8N_WEBHOOK_AUTH_VALUE=.*/N8N_WEBHOOK_AUTH_VALUE=$(openssl rand -hex 24)/" .env
sed -i "s/^CALLBACK_TOKEN=.*/CALLBACK_TOKEN=$(openssl rand -hex 24)/" .env
```

Luego abre `.env` (por ejemplo, `nano .env`) y completa la clave y los modelos de OpenRouter. La clave se crea en https://openrouter.ai/keys y va sin `Bearer`:

```
OPENROUTER_API_KEY=sk-or-v1-...
EMBEDDING_MODEL=openai/text-embedding-3-small
EMBEDDING_DIM=1536
LLM_MODEL=openai/gpt-4o-mini
```

`EMBEDDING_DIM` debe coincidir con la dimensión del modelo de embeddings elegido. Si algún puerto ya está ocupado en ese equipo, cámbialo en la misma sección del `.env`: `POSTGRES_PORT`, `BACKEND_PORT`, `FRONTEND_PORT`, `QDRANT_PORT` o `N8N_PORT`.

> El `.env` contiene secretos: Git lo ignora y **nunca** debe subirse al repositorio.

### 5. Levantar los servicios (backend, base de datos, API, n8n y Qdrant)

```bash
docker compose up -d --build --wait
docker compose ps
```

La primera vez tarda unos minutos, porque descarga las imágenes y compila el backend. Al terminar, `docker compose ps` debe mostrar `backend`, `postgres`, `qdrant` y `n8n` como **healthy**, y `frontend` como **Up**.

Comprueba que la API responde:

```bash
curl -s http://localhost:8080/api/v1/analytics/dashboard | head -c 200; echo
# Debe mostrar {"totalIncidents":150,"resolvedIncidents":130,...}
```

| Qué | Dirección |
|---|---|
| Página web | http://localhost:5500 |
| API REST | http://localhost:8080/api/v1 |
| Swagger (documentación de la API) | http://localhost:8080/swagger-ui.html |
| n8n | http://localhost:5678 |
| Panel de Qdrant | http://localhost:6333/dashboard |

Al arrancar, el backend crea las tablas y carga los datos iniciales en PostgreSQL. Si lo reinicias, conserva los datos y no los duplica.

### 6. Configurar n8n (solo la primera vez)

1. **Crear la cuenta de administrador.** Abre http://localhost:5678; n8n pide nombre, correo y contraseña. Es una cuenta local y no envía correos.
2. **Crear las credenciales** en *Credentials → Add credential → Header Auth*. La clave de OpenRouter no se configura aquí: los flujos la leen de `OPENROUTER_API_KEY` en el `.env` (paso 4).

   | Nombre de la credencial | Name | Value |
   |---|---|---|
   | `Webhook entrante (Spring Boot)` | `X-N8N-Token` | el valor de `N8N_WEBHOOK_AUTH_VALUE` del `.env` |
   | `Qdrant API Key` | `api-key` | cualquier texto (Qdrant no tiene clave en esta configuración) |

   Para ver el valor del token: `grep N8N_WEBHOOK_AUTH_VALUE .env`.
3. **Importar los dos flujos** desde *Workflows → Import from File*, con los archivos `n8n/workflows/01-indexacion-base-conocimiento.json` y `n8n/workflows/02-asistencia-rag.json`.
4. **Asignar las credenciales.** En cada flujo, abre los nodos con credencial (Webhook y Qdrant) y elige la que corresponde. En los JSON aparecen con el id `REEMPLAZAR`.
5. **Activar los flujos.** Guarda cada uno y enciende el interruptor **Active** (arriba a la derecha).
6. **Indexar la base de conocimiento** una vez. La colección de Qdrant se crea sola:

   ```bash
   TOKEN=$(grep ^N8N_WEBHOOK_AUTH_VALUE .env | cut -d= -f2)
   curl -s -X POST http://localhost:5678/webhook/index-knowledge-base \
     -H "X-N8N-Token: $TOKEN" -H "Content-Type: application/json" -d '{"all": true}'; echo
   # Debe responder {"status":"OK","indexed":[1,2,...],...}
   ```

Desde ese momento, el backend reindexa solo cada artículo que se edita, verifica, activa o desactiva.

Sin clave de OpenRouter el sistema funciona igual, pero las consultas al asistente terminan en el estado **Error en la IA** (`EMBEDDING_FALLO`).

### 7. Probar el sistema

**Recorrido manual**, en http://localhost:5500:

1. **Dashboard:** indicadores y gráficos; prueba los filtros por fecha, categoría y prioridad.
2. **Incidentes:** registra uno nuevo con *Nuevo incidente*, fíltralo y cambia su estado.
3. **Conocimiento:** crea un artículo, verifícalo y desactívalo. Con n8n activo, su vector se crea o se borra en Qdrant.
4. **Asistente IA:** elige un incidente abierto y consulta; la respuesta trae solución, pasos, fuentes y similitud.
5. **Historial:** revisa la consulta y califícala de 1 a 5.

**Pruebas automáticas**, sin instalar Java ni Node (se ejecutan en contenedores temporales):

```bash
# Backend: 27 pruebas de integración
docker run --rm -v "$PWD/backend":/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -q test

# Frontend: 23 pruebas unitarias
docker run --rm -v "$PWD":/app -w /app node:22-alpine node --test "frontend/tests/*.test.mjs"
```

**Comprobaciones rápidas de la API:**

```bash
curl -s "http://localhost:8080/api/v1/incidents?status=OPEN" | head -c 300; echo
curl -s "http://localhost:8080/api/v1/analytics/weekly-trends" | head -c 300; echo
curl -s -o /dev/null -w "Swagger: %{http_code}\n" http://localhost:8080/swagger-ui/index.html
```

La colección de Postman con 25 peticiones está en [docs/evidencias/postman_collection.json](docs/evidencias/postman_collection.json), y el detalle de todas las pruebas, en [docs/pruebas/README.md](docs/pruebas/README.md).

### 8. Usarlo desde otro dispositivo

Los servicios solo escuchan en `127.0.0.1` del servidor Ubuntu, así que no quedan expuestos en la red. Para usarlo desde otro computador, abre un **túnel SSH**. El servidor necesita `openssh-server`, que se instala con `sudo apt install -y openssh-server`.

```bash
# En el otro computador (Linux, macOS o Windows con PowerShell)
ssh -L 5500:localhost:5500 -L 8080:localhost:8080 -L 5678:localhost:5678 usuario@IP_DEL_SERVIDOR
```

Mientras esa sesión esté abierta, en ese computador funcionan http://localhost:5500 (la página), http://localhost:8080 (la API) y http://localhost:5678 (n8n), igual que en el servidor. El túnel del puerto 8080 es necesario porque el navegador llama a la API en `localhost:8080`.

### 9. Operación diaria

| Acción | Comando |
|---|---|
| Ver el estado | `docker compose ps` |
| Ver los logs del backend o de n8n | `docker compose logs -f backend` · `docker compose logs -f n8n` |
| Detener sin perder datos | `docker compose stop` |
| Volver a encender | `docker compose start` |
| Actualizar a la última versión | `git pull && docker compose up -d --build --wait` |
| Copia de seguridad de la base | `docker compose exec -T postgres pg_dump -U incidents incidents_db > respaldo.sql` |
| Borrar **todo**, incluidos los datos | `docker compose down -v` (irreversible) |

Docker vuelve a encender los contenedores al reiniciar el equipo solo si el servicio Docker arranca con el sistema (`sudo systemctl enable docker`) y los contenedores no se detuvieron a mano. Si no, ejecuta `docker compose start`.

### 10. Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `permission denied ... docker.sock` | Falta el grupo `docker`: ejecuta `sudo usermod -aG docker $USER` y vuelve a iniciar sesión |
| `port is already allocated` | Otro programa usa ese puerto: cámbialo en `.env` (sección de puertos) y ejecuta `docker compose up -d` |
| `required variable POSTGRES_PASSWORD is missing` | Falta el `.env`: repite el paso 4 |
| La página dice "API sin conexión" | El backend no está *healthy* (revisa `docker compose ps` y los logs), o la página se abrió como archivo (`file://`). En *Configuración*, la URL debe ser `http://localhost:8080` |
| Las consultas a la IA quedan en "Error de conexión" | n8n está apagado o el flujo 02 no está activo, o la credencial del webhook no coincide con `N8N_WEBHOOK_AUTH_VALUE` |
| Las consultas a la IA quedan en "Error en la IA" | Clave de OpenRouter ausente o inválida (revisa `OPENROUTER_API_KEY` y ejecuta `docker compose up -d n8n`), sin créditos, o `EMBEDDING_DIM` no coincide con el modelo. El mensaje del *Historial* indica la causa |
| El asistente siempre responde "Sin resultados" | No se indexó la base (paso 6.6) o `RAG_SCORE_THRESHOLD` es demasiado alto para el modelo elegido |
