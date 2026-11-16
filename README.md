# Sistema inteligente de análisis de incidentes tecnológicos y asistencia mediante IA

Plataforma para registrar incidentes de soporte, administrar una base de conocimiento técnica, analizar indicadores con SQL y obtener orientación de un asistente de IA que responde **solo** con artículos verificados (RAG). Tiene tres roles (administrador, técnico y usuario), cada uno con su propia interfaz, y sesiones con JWT.

![Dashboard del administrador: indicadores, filtros y gráficos calculados con SQL](docs/evidencias/frontend/02-admin-dashboard.png)

**Contenido**

1. [Arranque rápido](#1-arranque-rápido)
2. [Cuentas, roles y permisos](#2-cuentas-roles-y-permisos)
3. [Capturas de pantalla](#3-capturas-de-pantalla)
4. [Arquitectura](#4-arquitectura)
5. [n8n: configuración automática](#5-n8n-configuración-automática)
6. [Variables de entorno](#6-variables-de-entorno)
7. [Pruebas](#7-pruebas)
8. [Desarrollo sin Docker](#8-desarrollo-sin-docker)
9. [Comandos del día a día](#9-comandos-del-día-a-día)
10. [Despliegue en un servidor Ubuntu](#10-despliegue-en-un-servidor-ubuntu)
11. [Solución de problemas](#11-solución-de-problemas)
12. [Equipo, ramas y documentación](#12-equipo-ramas-y-documentación)

---

## 1. Arranque rápido

Todo corre en Docker: no hace falta instalar Java, Maven, Node, PostgreSQL ni n8n.

**Requisitos:** Git y Docker con Compose v2. En Windows y macOS es **Docker Desktop** (debe estar abierto). En Ubuntu, sigue el [paso 10.1](#101-instalar-docker).

### 1.1 Descargar el proyecto

```bash
git clone https://github.com/velascodazasergio-png/Sistema-inteligente-IA2.git incidentes-ia
cd incidentes-ia
```

¿Ya lo tenías clonado? Actualízalo con `git checkout main` y luego `git pull`.

### 1.2 Crear el `.env` con claves aleatorias

El `.env` guarda las contraseñas y los tokens. Git lo ignora y **nunca** debe subirse al repositorio.

**Windows (PowerShell):**

```powershell
Copy-Item .env.example .env
$text = [IO.File]::ReadAllText("$PWD\.env")
foreach ($k in 'POSTGRES_PASSWORD', 'N8N_WEBHOOK_AUTH_VALUE', 'CALLBACK_TOKEN', 'JWT_SECRET') {
  $v = -join ((1..48) | ForEach-Object { '{0:x}' -f (Get-Random -Maximum 16) })
  $text = $text -replace "(?m)^$k=[^\r\n]*", "$k=$v"
}
[IO.File]::WriteAllText("$PWD\.env", $text)   # sin BOM: Docker lee bien la primera línea
```

**Linux, macOS o Git Bash:**

```bash
cp .env.example .env
for k in POSTGRES_PASSWORD N8N_WEBHOOK_AUTH_VALUE CALLBACK_TOKEN JWT_SECRET; do
  sed -i.bak "s/^$k=.*/$k=$(openssl rand -hex 24)/" .env
done
rm -f .env.bak
```

Después abre el `.env` y pega tu clave de OpenRouter, que se crea en https://openrouter.ai/keys. Va sola, sin `Bearer`:

```
OPENROUTER_API_KEY=sk-or-v1-...
```

Los modelos ya vienen configurados: `openai/text-embedding-3-small` (1536 dimensiones) y `openai/gpt-4o-mini`. Sin la clave, todo funciona menos el asistente de IA.

### 1.3 Levantar todo

```bash
docker compose up -d --build --wait
docker compose ps
```

La primera vez tarda unos minutos, porque descarga las imágenes y compila el backend. Al terminar, `backend`, `postgres`, `qdrant`, `n8n` y `frontend` deben aparecer como **healthy**. `certs` aparece como *Exited (0)* porque solo crea el certificado y termina.

n8n **se configura solo** al arrancar: crea sus credenciales, importa el flujo y lo activa ([detalle](#5-n8n-configuración-automática)). Para comprobarlo:

```bash
docker compose logs n8n | grep -E "n8n-setup|Started"
# PowerShell:  docker compose logs n8n | Select-String "n8n-setup|Started"
```

Debe mostrar `[n8n-setup] Done.` y el flujo `Flujos 01 + 02 combinados …` como `Started`.

### 1.4 Indexar la base de conocimiento (una sola vez)

Así se cargan los artículos verificados en Qdrant, la base vectorial. La colección se crea sola.

**Windows (PowerShell):**

```powershell
$token = ((Select-String -Path .env -Pattern '^N8N_WEBHOOK_AUTH_VALUE=').Line -split '=', 2)[1]
Invoke-RestMethod -Method Post -Uri http://localhost:5678/webhook/index-knowledge-base `
  -Headers @{ 'X-N8N-Token' = $token } -ContentType 'application/json' -Body '{"all": true}'
```

**Linux, macOS o Git Bash:**

```bash
TOKEN=$(grep ^N8N_WEBHOOK_AUTH_VALUE .env | cut -d= -f2)
curl -s -X POST http://localhost:5678/webhook/index-knowledge-base \
  -H "X-N8N-Token: $TOKEN" -H "Content-Type: application/json" -d '{"all": true}'; echo
```

Debe responder `status: OK` y la lista de artículos indexados. Desde ese momento, el backend reindexa solo cada artículo que se crea, edita, verifica, activa o desactiva.

### 1.5 Abrir la aplicación

| Qué | Dirección |
|---|---|
| **Aplicación web** | http://localhost:5500 · https://localhost:5443 |
| API REST | http://localhost:8080/api/v1 |
| Swagger (documentación de la API) | http://localhost:8080/swagger-ui.html |
| n8n (editor de flujos) | http://localhost:5678 |
| Panel de Qdrant | http://localhost:6333/dashboard |

Para entrar, usa una de las [cuentas de demostración](#2-cuentas-roles-y-permisos), por ejemplo `sarah.jenkins@enterprise.io` con la contraseña `Soporte2026*`.

Si cambiaste un puerto en el `.env` (sección de puertos), usa ese en lugar del predeterminado.

> Por HTTPS el navegador muestra un aviso, porque el certificado de desarrollo es autofirmado. Acéptalo una vez (*Avanzado → Continuar*).

---

## 2. Cuentas, roles y permisos

Para entrar hay que iniciar sesión. `POST /api/v1/auth/login` devuelve un **JWT** firmado con HS256 que dura 8 horas, y el frontend lo envía en `Authorization: Bearer <token>` en cada petición. Cada rol ve su propia interfaz, y el backend vuelve a comprobar cada permiso: ocultar un botón no basta.

**Cuentas de demostración** (usuarios de `data.sql`). Todas usan la contraseña `DEMO_PASSWORD`, que por defecto es **`Soporte2026*`**:

| Rol | Cuentas (área) |
|---|---|
| Administrador (`ADMIN`) | `sarah.jenkins@enterprise.io` (IT Management) |
| Técnico de soporte (`SUPPORT_TECHNICIAN`) | `marcus.vance@enterprise.io` · `elena.rostova@enterprise.io` · `david.kim@enterprise.io` (IT Support) |
| Usuario (`EMPLOYEE`) | `laura.chen@enterprise.io` (Finance) · `carlos.mendoza@enterprise.io` (Sales) · `hannah.abbott@enterprise.io` (Human Resources) · `liam.oconnor@enterprise.io` (Engineering) · `priya.patel@enterprise.io` (Marketing) · `noah.williams@enterprise.io` (Operations) |

| Sección o acción | Administrador | Técnico | Usuario |
|---|:-:|:-:|:-:|
| Dashboard y analítica | ✔ | ✔ | — |
| Incidentes | Todos | Todos | **Mis incidentes**: solo los suyos |
| Reportar incidente | A nombre de cualquiera | A nombre de cualquiera | Siempre a su nombre |
| Cambiar estado de un incidente | ✔ | ✔ | — |
| Base de conocimiento | Crear, editar, **verificar y activar** | Crear y editar | Leer artículos publicados |
| Asistente IA | Cualquier incidente | Cualquier incidente | Solo sus incidentes |
| Historial y calificación | Todo; puede calificar en nombre de otro | Todo; califica con su usuario | **Mis consultas**: las de sus incidentes |
| Configuración y catálogo de componentes | ✔ | — | — |
| Sección de inicio | Dashboard | Dashboard | Mis incidentes |

Seguridad:

- **Contraseñas:** se guardan como hash BCrypt, nunca en texto plano. Al arrancar, el backend asigna `DEMO_PASSWORD` a los usuarios que todavía no tienen contraseña, y nunca sobrescribe las que ya existen. Fuera de una demostración, cámbiala en el `.env`.
- **Inicio de sesión:** un correo inexistente y una contraseña incorrecta reciben la misma respuesta (`401`, *Correo o contraseña incorrectos*). Así no se puede averiguar qué correos están registrados.
- **Errores:** sin token, o con un token vencido o alterado, la respuesta es `401` y el frontend vuelve al login. Si el rol no alcanza o el recurso es de otra persona, la respuesta es `403`.
- **n8n** no inicia sesión. Para leer artículos usa el `X-Callback-Token`, que le da un rol de servicio limitado a esa lectura.
- **Rutas públicas:** solo el login, `/api-docs` y `/swagger-ui`. Para usar Swagger, ejecuta `POST /api/v1/auth/login`, pulsa **Authorize** y pega el `accessToken`.

---

## 3. Capturas de pantalla

Tomadas de la aplicación en funcionamiento, con los datos iniciales y respuestas reales del asistente. Están en [docs/evidencias/frontend/](docs/evidencias/frontend/).

### Inicio de sesión

Cada cuenta de demostración rellena el correo con un clic. Después del login, cada rol llega a su propia sección de inicio.

![Inicio de sesión con las tres cuentas de demostración, una por rol](docs/evidencias/frontend/01-login.png)

### Administrador

**Dashboard** (la imagen del inicio de este README). Indicadores, evolución semanal y mensual, tiempos de resolución y distribución por categoría y estado, con filtros por fecha, categoría y prioridad.

| **Incidentes:** todos los incidentes, con filtros, búsqueda y orden | **Detalle:** datos del incidente, cambio de estado y accesos a la IA |
|:-:|:-:|
| ![Lista de incidentes](docs/evidencias/frontend/03-admin-incidentes.png) | ![Detalle de un incidente](docs/evidencias/frontend/04-admin-incidente-detalle.png) |

| **Nuevo incidente:** puede registrarlo a nombre de cualquier usuario | **Base de conocimiento:** artículos activos, pendientes e inactivos |
|:-:|:-:|
| ![Formulario de nuevo incidente](docs/evidencias/frontend/05-admin-nuevo-incidente.png) | ![Base de conocimiento](docs/evidencias/frontend/06-admin-conocimiento.png) |

| **Artículo:** solo el administrador lo verifica (o le quita la verificación) y lo desactiva | **Historial:** todas las consultas a la IA, su estado y su calificación |
|:-:|:-:|
| ![Detalle de un artículo con verificar y desactivar](docs/evidencias/frontend/07-admin-articulo.png) | ![Historial de consultas](docs/evidencias/frontend/09-admin-historial.png) |

| **Analítica:** indicadores y gráficos calculados con SQL | **Configuración:** conexión con la API (solo administrador) |
|:-:|:-:|
| ![Analítica](docs/evidencias/frontend/10-admin-analitica.png) | ![Configuración](docs/evidencias/frontend/11-admin-configuracion.png) |

**Asistente IA (RAG).** La respuesta trae diagnóstico, solución, pasos, confianza y los artículos usados como fuente, con su similitud.

![Asistente IA con una respuesta real](docs/evidencias/frontend/08-admin-asistente.png)

### Técnico de soporte

Ve el dashboard, los incidentes, la base de conocimiento, el asistente y el historial. No ve *Configuración* y no puede verificar artículos: solo crearlos y editarlos.

| **Dashboard del técnico:** sin la sección Configuración en el menú | **Artículo:** puede editarlo, pero la verificación queda para el administrador |
|:-:|:-:|
| ![Dashboard del técnico](docs/evidencias/frontend/12-tecnico-dashboard.png) | ![Artículo visto por el técnico, sin botón de verificar](docs/evidencias/frontend/13-tecnico-articulo.png) |

### Usuario

Solo ve *Mis incidentes*, *Conocimiento* (lectura de artículos publicados), *Asistente IA* sobre sus incidentes y *Mis consultas*.

| **Mis incidentes:** solo los que reportó | **Reportar incidente:** siempre queda a su nombre |
|:-:|:-:|
| ![Mis incidentes](docs/evidencias/frontend/14-usuario-mis-incidentes.png) | ![Formulario para reportar un incidente](docs/evidencias/frontend/15-usuario-reportar.png) |

| **Mis consultas:** las consultas a la IA sobre sus incidentes | **Conocimiento:** solo artículos activos y verificados, sin edición |
|:-:|:-:|
| ![Mis consultas](docs/evidencias/frontend/16-usuario-mis-consultas.png) | ![Base de conocimiento en modo lectura](docs/evidencias/frontend/17-usuario-conocimiento.png) |

### Móvil

| Dashboard | Menú lateral | Inicio de sesión |
|:-:|:-:|:-:|
| <img src="docs/evidencias/frontend/18-movil-dashboard.png" alt="Dashboard en móvil" width="240"> | <img src="docs/evidencias/frontend/19-movil-menu.png" alt="Menú lateral en móvil" width="240"> | <img src="docs/evidencias/frontend/20-movil-login.png" alt="Inicio de sesión en móvil" width="240"> |

### API (Swagger)

Los 26 endpoints documentados por módulo. Con **Authorize** se pega el JWT de `POST /api/v1/auth/login`.

![Swagger UI con los endpoints de la API](docs/evidencias/swagger-ui.png)

---

## 4. Arquitectura

```
┌────────────┐  REST   ┌──────────────────┐  JDBC  ┌──────────────┐
│  Frontend  │ ──────▶ │  Backend Spring  │ ─────▶ │ PostgreSQL / │
│ HTML/CSS/JS│ ◀────── │ Boot + JWT (API) │ ◀───── │ H2 (desarr.) │
└────────────┘  Bearer └──────────────────┘        └──────────────┘
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
- El **backend** registra cada consulta como `PENDING` y la envía al webhook de n8n. n8n devuelve la respuesta, o el fallo, por callback.
- **n8n** busca en Qdrant los artículos más parecidos, comprueba en Spring Boot que sigan activos y verificados, y genera la respuesta con el LLM. Si no hay artículos relevantes, lo dice en lugar de inventar.

Estados de una consulta a la IA:

- **Pendiente** (`PENDING`)
- **Respondida** (`COMPLETED`)
- **Sin resultados** (`NO_RESULTS`)
- **Error** (`COMMUNICATION_ERROR` si n8n no responde; `FAILED` si n8n falla al procesar)

### Servicios de Docker Compose

| Servicio | Imagen | Función | Puerto |
|---|---|---|---|
| `postgres` | postgres:16.4-alpine | Base de datos relacional | 5432 |
| `backend` | se construye desde `backend/` | API REST Spring Boot | 8080 |
| `certs` | alpine:3.20 | Crea un certificado autofirmado la primera vez (y termina) | — |
| `frontend` | nginx:1.27-alpine | Sirve la página por HTTP y HTTPS y reenvía `/api` al backend | 5500 y 5443 |
| `qdrant` | qdrant/qdrant:v1.12.4 | Base de datos vectorial | 6333 |
| `n8n` | n8nio/n8n:1.80.0 | Flujos de indexación y RAG (se configura solo) | 5678 |

Por seguridad, los puertos se publican **solo en `127.0.0.1`**. Si alguno está ocupado en tu equipo, cámbialo en el `.env`: `POSTGRES_PORT`, `BACKEND_PORT`, `FRONTEND_PORT`, `FRONTEND_HTTPS_PORT`, `QDRANT_PORT` o `N8N_PORT`.

### Requisitos funcionales

| RF | Funcionalidad | Dónde |
|---|---|---|
| RF01 | Registro y gestión de incidentes | `IncidentController` · `#/incidentes` |
| RF02 | Base de conocimiento (activos y verificados) | `KnowledgeArticleController` · `#/conocimiento` |
| RF03 | Indexación de artículos en Qdrant | Flujo n8n 01 |
| RF04, RF05 | Búsqueda semántica y asistente RAG | Flujo n8n 02 · `AiAssistanceController` · `#/asistente` |
| RF06 | Indicadores calculados con SQL | `AnalyticsController` · `#/analitica` |
| RF07 | Dashboard con filtros | `#/dashboard` |
| RF08 | Historial y calificación de respuestas | `#/historial` |
| — | Inicio de sesión, roles y permisos (JWT) | `security/` · `AuthController` · `#/login` |

### Estructura del repositorio

```
.
├── docker-compose.yml          # Servicios, healthchecks y volúmenes
├── .env.example                # Plantilla de configuración (copiar a .env)
├── .gitattributes              # Scripts .sh siempre con fin de línea LF
├── backend/                    # API Spring Boot 3.4 · Java 21
│   ├── Dockerfile
│   └── src/
│       ├── main/java/com/mafe/incidentai/
│       │   ├── controller/     # Endpoints REST (auth, incidentes, conocimiento, IA, analítica, catálogos)
│       │   ├── security/       # JWT, reglas por rol, acceso a recursos propios, token de servicio de n8n
│       │   ├── service/        # Reglas de negocio
│       │   ├── repository/     # JPA y consultas analíticas
│       │   ├── domain/  dto/   # Entidades y contratos de entrada y salida
│       │   ├── client/         # Llamadas a los webhooks de n8n
│       │   ├── exception/      # Errores en JSON (400, 401, 403, 404, 409)
│       │   └── config/         # CORS, callbacks, cliente HTTP, OpenAPI
│       ├── main/resources/     # application.properties, data.sql
│       └── test/               # 60 pruebas de integración
├── frontend/                   # HTML, CSS y JavaScript sin frameworks (ver frontend/README.md)
│   ├── index.html  css/  js/  assets/
│   └── tests/                  # 44 pruebas unitarias (node:test)
├── n8n/
│   ├── workflows/              # 01 indexación · 02 asistencia RAG · 03 combinado (el que se activa)
│   ├── setup.sh                # Configura n8n al arrancar
│   └── combinar_flujos.py      # Regenera el 03 a partir del 01 y el 02
├── database/data.sql           # Copia de los datos iniciales
├── certs/                      # Certificado HTTPS (se crea solo; Git lo ignora)
├── docs/                       # Diagramas, informe de análisis, pruebas, evidencias y presentación
└── AI_USAGE.md                 # Uso de IA durante el desarrollo
```

---

## 5. n8n: configuración automática

Cada vez que arranca el contenedor `n8n`, antes de `n8n start`, se ejecuta [n8n/setup.sh](n8n/setup.sh). El script:

1. crea o actualiza las credenciales `Webhook entrante (Spring Boot)`, con `N8N_WEBHOOK_AUTH_HEADER` y `N8N_WEBHOOK_AUTH_VALUE` del `.env`, y `Qdrant API Key`;
2. importa `n8n/workflows/03-flujos-combinados.json` con esas credenciales ya asignadas;
3. desactiva cualquier otro flujo, porque dos webhooks con la misma ruta impiden activar, y activa el combinado.

No hay que importar, asignar credenciales ni activar nada a mano. La clave de OpenRouter tampoco va en n8n: los flujos la leen de `OPENROUTER_API_KEY`. Si falta `N8N_WEBHOOK_AUTH_VALUE`, n8n no arranca, y el motivo aparece en `docker compose logs n8n`.

**Cuenta del editor.** La primera vez que abras http://localhost:5678, n8n pide crear un administrador (nombre, correo y contraseña). Es una cuenta local, no envía correos, y solo sirve para entrar al editor: los webhooks funcionan aunque no exista.

**Editar los flujos.** El repositorio es la fuente de verdad, y en cada arranque se vuelve a importar el flujo combinado. Un cambio hecho solo en la interfaz de n8n se pierde al reiniciar. Para cambiar un flujo:

```bash
# 1. Edita n8n/workflows/01-... o 02-... (o expórtalo desde n8n y reemplaza el archivo)
# 2. Regenera el combinado y reinicia n8n
python n8n/combinar_flujos.py
docker compose restart n8n
```

Para trabajar en la interfaz de n8n sin que se sobrescriba, pon `N8N_AUTO_SETUP=false` en el `.env` y ejecuta `docker compose up -d n8n`.

| Webhook | Flujo | Lo llama |
|---|---|---|
| `POST /webhook/index-knowledge-base` | 01: indexación en Qdrant | El backend, al cambiar un artículo; o tú, con `{"all": true}` |
| `POST /webhook/analyze-incident` | 02: asistencia RAG | El backend, cuando alguien consulta al asistente |

Los dos exigen el encabezado `X-N8N-Token`. Sin él, o con un valor incorrecto, responden `403`.

---

## 6. Variables de entorno

Se definen en el `.env`. Las marcadas con **\*** son obligatorias para levantar con Docker; el resto tiene un valor por defecto.

| Variable | Usada por | Descripción |
|---|---|---|
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`\* | postgres, backend | Base de datos |
| `N8N_WEBHOOK_AUTH_HEADER`, `N8N_WEBHOOK_AUTH_VALUE`\* | backend y n8n | Token con el que el backend llama a n8n. n8n crea su credencial con estos valores al arrancar |
| `CALLBACK_TOKEN` | backend y n8n | Token `X-Callback-Token` de los callbacks. También identifica a n8n cuando lee artículos. Si está vacío, no se exige |
| `JWT_SECRET` | backend | Clave HS256 de las sesiones (mínimo 32 caracteres). Si está vacía, se genera una aleatoria en cada arranque y las sesiones se cierran al reiniciar |
| `JWT_EXPIRATION_MINUTES` | backend | Duración de la sesión. Por defecto 480 (8 horas) |
| `DEMO_PASSWORD` | backend | Contraseña inicial de los usuarios de `data.sql`. Por defecto `Soporte2026*` |
| `CORS_ALLOWED_ORIGINS` | backend | Orígenes permitidos para el frontend |
| `OPENROUTER_API_KEY` | n8n | Clave de OpenRouter, sin `Bearer`. Tras cambiarla: `docker compose up -d n8n` |
| `OPENROUTER_BASE_URL`, `EMBEDDING_MODEL`, `EMBEDDING_DIM`, `LLM_MODEL` | n8n | Modelos de embeddings y de chat. `EMBEDDING_DIM` debe coincidir con el modelo de embeddings |
| `RAG_SCORE_THRESHOLD`, `RAG_TOP_K`, `MIN_CHARS_DESCRIPCION` | n8n | Umbral de similitud (0,55), número de fuentes y largo mínimo de la descripción |
| `QDRANT_COLLECTION` | n8n | Colección de Qdrant |
| `N8N_AUTO_SETUP` | n8n | `true` (por defecto): configura n8n al arrancar. `false`: no lo toca |
| `TZ` | postgres, n8n | Zona horaria. Por defecto `America/Bogota` |
| `*_PORT` | Docker | Puertos del equipo, si los predeterminados están ocupados |

Fuera de Docker (sección 8), el backend también lee `DB_URL`, `DB_DRIVER`, `DB_USERNAME`, `DB_PASSWORD`, `N8N_WEBHOOK_URL` y `N8N_INDEX_WEBHOOK_URL`.

Las claves nunca se escriben en el código ni en el repositorio: van en el `.env`.

---

## 7. Pruebas

### Automáticas (sin instalar Java ni Node)

Se ejecutan en contenedores temporales, desde la raíz del repositorio.

```bash
# Backend: 60 pruebas de integración (MockMvc sobre H2)
docker run --rm -v "$PWD/backend":/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -q -B test

# Frontend: 44 pruebas unitarias
docker run --rm -v "$PWD/frontend":/app -w /app node:22-alpine node --test "tests/*.test.mjs"
```

En PowerShell, cambia `"$PWD/backend"` por `"${PWD}\backend"` y `"$PWD/frontend"` por `"${PWD}\frontend"`. Con Java 21 y Maven, o con Node 22, instalados en el equipo, también puedes usar `cd backend && mvn test` y `node --test "frontend/tests/*.test.mjs"`.

Las pruebas del backend cubren:

- el inicio de sesión y los permisos de cada rol con tokens reales (`SecurityTest`);
- las validaciones y la regla de incidentes cerrados;
- los filtros de analítica y la verificación de artículos;
- los estados `NO_RESULTS` y `FAILED`;
- una sola calificación por respuesta;
- el token de callback;
- el aviso de reindexación a n8n, que se envía solo después del commit.

### Recorrido manual

En http://localhost:5500:

1. **Administrador** (`sarah.jenkins@enterprise.io`):
   - en el dashboard, revisa los indicadores y filtra por fecha, categoría y prioridad;
   - registra un incidente y cambia su estado;
   - crea un artículo, verifícalo y desactívalo.
2. **Asistente IA:** elige un incidente abierto y consulta. La respuesta trae solución, pasos, fuentes y similitud. Califícala en *Historial*.
3. **Técnico** (`marcus.vance@enterprise.io`): no ve *Configuración* ni puede verificar artículos.
4. **Usuario** (`laura.chen@enterprise.io`): solo ve *Mis incidentes*, *Conocimiento* (lectura), *Asistente IA* y *Mis consultas*.

### Comprobaciones de la API

**Linux, macOS o Git Bash:**

```bash
TOKEN=$(curl -s http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" \
  -d '{"email":"sarah.jenkins@enterprise.io","password":"Soporte2026*"}' | sed 's/.*"accessToken":"\([^"]*\)".*/\1/')
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/analytics/dashboard | head -c 200; echo
curl -s -o /dev/null -w "Sin token: %{http_code}\n" http://localhost:8080/api/v1/incidents   # 401
```

**Windows (PowerShell):**

```powershell
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login -ContentType 'application/json' `
  -Body '{"email":"sarah.jenkins@enterprise.io","password":"Soporte2026*"}'
Invoke-RestMethod -Uri http://localhost:8080/api/v1/analytics/dashboard -Headers @{ Authorization = "Bearer $($login.accessToken)" }
```

Con los datos iniciales, `totalIncidents` es 150.

- Colección de Postman (25 peticiones): [docs/evidencias/postman_collection.json](docs/evidencias/postman_collection.json).
- Detalle de todas las pruebas: [docs/pruebas/README.md](docs/pruebas/README.md).

---

## 8. Desarrollo sin Docker

Para trabajar en el backend o el frontend con recarga rápida. Requiere Java 21, Maven 3.9 y Python 3 (u otro servidor estático).

```bash
# Backend en http://localhost:8080 (H2 en memoria con los datos iniciales)
cd backend
mvn spring-boot:run

# Frontend en http://localhost:5500 (en otra terminal)
cd frontend
python -m http.server 5500
```

- Consola H2: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:incidents_db`, usuario `sa`).
- Sin n8n, las consultas a la IA quedan en *Error de conexión*. El resto del sistema funciona.

---

## 9. Comandos del día a día

| Acción | Comando |
|---|---|
| Levantar todo (o aplicar cambios del `.env`) | `docker compose up -d --build --wait` |
| Ver el estado | `docker compose ps` |
| Ver los logs | `docker compose logs -f backend` · `docker compose logs -f n8n` |
| Reiniciar un servicio | `docker compose restart n8n` |
| Detener sin perder datos | `docker compose stop` |
| Volver a encender | `docker compose start` |
| Actualizar a la última versión | `git pull` y luego `docker compose up -d --build --wait` |
| Reindexar la base de conocimiento | Comando del [paso 1.4](#14-indexar-la-base-de-conocimiento-una-sola-vez) |
| Copia de seguridad de la base | `docker compose exec -T postgres pg_dump -U incidents incidents_db > respaldo.sql` |
| Borrar **todo**, incluidos los datos | `docker compose down -v` (irreversible) |

---

## 10. Despliegue en un servidor Ubuntu

Probado en Ubuntu 22.04 y 24.04.

### 10.1 Instalar Docker

```bash
sudo apt update
sudo apt install -y ca-certificates curl git openssl

sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
  https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER          # usar Docker sin sudo (cierra sesión y vuelve a entrar)
sudo systemctl enable docker           # que Docker arranque con el sistema
```

Para comprobarlo, `docker --version` y `docker compose version` deben responder sin error. Después, sigue el [arranque rápido](#1-arranque-rápido) (pasos 1.1 a 1.4).

### 10.2 HTTPS

La página también se sirve en **https://localhost:5443**. Por HTTPS, nginx reenvía `/api`, `/api-docs` y `/swagger-ui` al backend: la página y la API comparten origen, así que no hay contenido mixto ni hace falta CORS.

- **Certificado de desarrollo:** la primera vez, el servicio `certs` crea uno autofirmado para `localhost` en `certs/`.
- **Certificado real (servidor con dominio):** obtenlo con Let's Encrypt y cópialo en `certs/`. El servicio `certs` ve que ya existe y no lo reemplaza:

  ```bash
  sudo apt install -y certbot
  sudo certbot certonly --standalone -d tu-dominio.com
  sudo cp /etc/letsencrypt/live/tu-dominio.com/fullchain.pem certs/fullchain.pem
  sudo cp /etc/letsencrypt/live/tu-dominio.com/privkey.pem  certs/privkey.pem
  docker compose restart frontend
  ```

  Para publicarlo en internet, en `docker-compose.yml` cambia el puerto HTTPS del frontend de `127.0.0.1:${FRONTEND_HTTPS_PORT:-5443}:443` a `443:443`, y abre el puerto con `sudo ufw allow 443`. El resto de servicios debe seguir en `127.0.0.1`.
- **Encabezados de seguridad:** HSTS (solo por HTTPS), `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy` y `Permissions-Policy`. nginx no expone su versión.

### 10.3 Usarlo desde otro computador

Los servicios solo escuchan en `127.0.0.1` del servidor. Para usarlos desde otro equipo, abre un **túnel SSH**. El servidor necesita `openssh-server`:

```bash
# En el otro computador (Linux, macOS o Windows con PowerShell)
ssh -L 5500:localhost:5500 -L 5443:localhost:5443 -L 8080:localhost:8080 -L 5678:localhost:5678 usuario@IP_DEL_SERVIDOR
```

Mientras la sesión esté abierta, en ese computador funcionan http://localhost:5500, http://localhost:8080 y http://localhost:5678, igual que en el servidor. El túnel del 8080 es necesario por HTTP, porque el navegador llama a la API en `localhost:8080`. Por HTTPS (5443) basta con ese túnel.

---

## 11. Solución de problemas

Lo primero es revisar `docker compose ps` y los logs del servicio que falla (`docker compose logs --tail 50 <servicio>`).

| Síntoma | Causa y solución |
|---|---|
| `required variable POSTGRES_PASSWORD is missing` | Falta el `.env`. Repite el [paso 1.2](#12-crear-el-env-con-claves-aleatorias) |
| `port is already allocated` | Otro programa usa ese puerto (por ejemplo, otro n8n en el 5678). Cámbialo en el `.env` (`N8N_PORT`, `BACKEND_PORT`, …) y ejecuta `docker compose up -d` |
| `permission denied ... docker.sock` (Linux) | Falta el grupo `docker`: ejecuta `sudo usermod -aG docker $USER` y vuelve a iniciar sesión |
| `error during connect` / `cannot find the file specified` (Windows) | Docker Desktop no está abierto |
| n8n se reinicia y el log dice `N8N_WEBHOOK_AUTH_VALUE is missing` | Completa el token en el `.env` (paso 1.2) y ejecuta `docker compose up -d` |
| El log de n8n muestra `\r` o `not found` | Copia antigua del repositorio con fin de línea de Windows. Ejecuta `git pull` y `docker compose up -d` |
| El webhook de n8n responde `403 Authorization data is wrong!` | El token no coincide. Revisa `N8N_WEBHOOK_AUTH_VALUE` y ejecuta `docker compose up -d backend n8n` |
| El webhook de n8n responde `404` | El flujo no está activo. Mira `docker compose logs n8n` y ejecuta `docker compose restart n8n` |
| Cambios en el editor de n8n desaparecen al reiniciar | Es lo esperado: el flujo se reimporta del repositorio ([sección 5](#5-n8n-configuración-automática)). Usa `N8N_AUTO_SETUP=false` para editar en la interfaz |
| La página dice "API sin conexión" | El backend no está *healthy*, o la página se abrió como archivo (`file://`). En *Configuración*, la URL debe ser `http://localhost:8080` |
| El login dice *Correo o contraseña incorrectos* | La contraseña es la de `DEMO_PASSWORD` (`Soporte2026*`). Si cambiaste `DEMO_PASSWORD` después del primer arranque, sigue valiendo la anterior: solo se asigna a usuarios sin contraseña |
| La sesión se cierra cada vez que se reinicia el backend | `JWT_SECRET` está vacío en el `.env` (paso 1.2) |
| Las consultas a la IA quedan en "Error de conexión" | n8n está apagado o sin el flujo activo. Revisa `docker compose logs n8n` |
| Las consultas a la IA quedan en "Error en la IA" | Falta `OPENROUTER_API_KEY`, no es válida o no tiene saldo; o `EMBEDDING_DIM` no coincide con el modelo. Tras corregir, ejecuta `docker compose up -d n8n`. El *Historial* indica la causa |
| El asistente siempre responde "Sin resultados" | No se indexó la base ([paso 1.4](#14-indexar-la-base-de-conocimiento-una-sola-vez)) o `RAG_SCORE_THRESHOLD` es demasiado alto para el modelo |

---

## 12. Equipo, ramas y documentación

| Rama | Responsable | Alcance |
|---|---|---|
| `mafe` | Mafeqm | Backend Spring Boot, modelo de datos, datos iniciales |
| `dev-Lisseth` | lissethkatherine | Flujos n8n (indexación y RAG) |
| `devsergy` | velascodazasergio-png | Frontend, roles y JWT, integración frontend ↔ backend ↔ n8n |
| `main` | — | Integración final |

| Documento | Contenido |
|---|---|
| [frontend/README.md](frontend/README.md) | Estructura del frontend, rutas y componentes |
| [docs/diagramas/](docs/diagramas/) | Modelo entidad-relación, arquitectura y secuencia del flujo RAG (Mermaid) |
| [docs/informe-analisis/](docs/informe-analisis/) | Informe de análisis de datos con las consultas SQL, resultados y conclusiones |
| [docs/pruebas/](docs/pruebas/README.md) | Pruebas de integración, API, flujo de IA, roles, Docker e interfaz |
| [docs/evidencias/](docs/evidencias/) | Swagger, OpenAPI, colección de Postman y capturas |
| [docs/presentacion/](docs/presentacion/) | Presentación final (HTML; se abre con doble clic) |
| [AI_USAGE.md](AI_USAGE.md) | Uso de herramientas de IA durante el desarrollo |
