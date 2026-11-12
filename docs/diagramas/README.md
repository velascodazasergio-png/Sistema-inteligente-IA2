# Diagramas

Los diagramas están escritos en [Mermaid](https://mermaid.js.org/), así que GitHub los muestra directamente. Se generaron a partir del código: entidades JPA en `backend/src/main/java/com/mafe/incidentai/domain/entity` y flujos en `n8n/workflows`.

## 1. Modelo entidad-relación

```mermaid
erDiagram
    USERS ||--o{ INCIDENTS : "reporta"
    CATEGORIES ||--o{ INCIDENTS : "clasifica"
    CATEGORIES ||--o{ KNOWLEDGE_ARTICLES : "agrupa"
    USERS |o--o{ KNOWLEDGE_ARTICLES : "redacta"
    INCIDENTS ||--o{ AI_QUERIES : "genera"
    AI_QUERIES ||--o| AI_RESPONSES : "recibe"
    AI_RESPONSES ||--o{ RESPONSE_SOURCES : "cita"
    KNOWLEDGE_ARTICLES |o--o{ RESPONSE_SOURCES : "respalda"
    AI_RESPONSES ||--o| RATINGS : "es calificada"
    USERS ||--o{ RATINGS : "evalúa"

    USERS {
        bigint id PK
        varchar full_name
        varchar email UK
        varchar role "EMPLOYEE | SUPPORT_TECHNICIAN | ADMIN"
        timestamp created_at
    }
    CATEGORIES {
        bigint id PK
        varchar name UK
        varchar description
        boolean is_active
    }
    INCIDENTS {
        bigint id PK
        varchar title
        text description
        varchar priority "LOW | MEDIUM | HIGH | CRITICAL"
        varchar status "OPEN | IN_AI_ANALYSIS | ASSIGNED_TO_TECH | RESOLVED | CLOSED"
        timestamp created_at
        timestamp updated_at
        bigint user_id FK
        bigint category_id FK
    }
    KNOWLEDGE_ARTICLES {
        bigint id PK
        varchar title
        text content "texto que indexa n8n"
        text problem
        text symptoms
        text cause
        text solution
        varchar reference_url
        boolean is_active
        boolean is_verified
        timestamp created_at
        timestamp updated_at
        bigint category_id FK
        bigint author_user_id FK "opcional"
    }
    AI_QUERIES {
        bigint id PK
        text sent_prompt
        varchar query_status "PENDING | COMPLETED | NO_RESULTS | COMMUNICATION_ERROR | FAILED"
        timestamp queried_at
        varchar failure_code
        varchar failure_message
        bigint incident_id FK
    }
    AI_RESPONSES {
        bigint id PK
        text suggested_diagnosis
        text solution_steps
        double confidence_score
        bigint processing_time_ms
        timestamp responded_at
        bigint ai_query_id FK,UK
    }
    RESPONSE_SOURCES {
        bigint id PK
        varchar source_title
        varchar reference_url
        text cited_snippet
        double similarity_score
        bigint ai_response_id FK
        bigint knowledge_article_id FK "opcional"
    }
    RATINGS {
        bigint id PK
        int score "1 a 5"
        boolean is_helpful
        varchar feedback_comment
        timestamp rated_at
        bigint ai_response_id FK,UK
        bigint evaluator_user_id FK
    }
```

Reglas de negocio reflejadas en el modelo:

- Un incidente puede tener varias consultas a la IA; cada consulta tiene como máximo **una** respuesta (`ai_query_id` único).
- Cada respuesta tiene como máximo **una** calificación (`ai_response_id` único en `ratings`).
- Una fuente puede apuntar a un artículo de la base de conocimiento. Solo los artículos `is_active` e `is_verified` se usan en el RAG.
- No se registran incidentes ni artículos nuevos en una categoría inactiva, y un incidente `CLOSED` ya no cambia de estado ni se consulta con la IA.

## 2. Arquitectura

```mermaid
flowchart LR
    U([Usuario]) --> FE

    subgraph Cliente
        FE["Frontend<br/>HTML · CSS · JS<br/>(nginx :5500)"]
    end

    subgraph Servidor
        BE["Backend Spring Boot :8080<br/>API REST · JPA · Swagger"]
        DB[("PostgreSQL / H2<br/>datos relacionales")]
    end

    subgraph IA["Orquestación de IA"]
        N8N["n8n :5678<br/>Flujo 01 indexación<br/>Flujo 02 RAG"]
        QD[("Qdrant :6333<br/>vectores")]
        OR["OpenRouter<br/>embeddings + LLM"]
    end

    FE -- "REST /api/v1" --> BE
    BE -- JDBC --> DB
    BE -- "POST webhook<br/>+ Header Auth" --> N8N
    N8N -- "callback respuesta / fallo<br/>+ X-Callback-Token" --> BE
    N8N -- "GET artículos" --> BE
    N8N --> QD
    N8N --> OR
```

El frontend solo se comunica con el backend. Las claves de OpenRouter y Qdrant viven en las credenciales de n8n, y los tokens de integración en variables de entorno.

## 3. Secuencia de una consulta al asistente (RF04, RF05)

```mermaid
sequenceDiagram
    autonumber
    actor U as Usuario
    participant FE as Frontend
    participant BE as Backend
    participant DB as Base de datos
    participant N as n8n (flujo 02)
    participant Q as Qdrant
    participant L as OpenRouter

    U->>FE: Elige incidente y envía consulta
    FE->>BE: POST /ai-assistance/request
    BE->>DB: Guarda AiQuery (PENDING) e incidente → IN_AI_ANALYSIS
    BE->>N: POST webhook (Header Auth)
    alt n8n no responde
        BE->>DB: AiQuery → COMMUNICATION_ERROR
    end
    BE-->>FE: 202 { aiQueryId, queryStatus }
    loop cada 3 s, máximo 90 s
        FE->>BE: GET /ai-assistance/incidents/{id}/history
    end
    N->>L: Embedding de la descripción
    N->>Q: Búsqueda vectorial (top K, umbral)
    N->>BE: GET /knowledge-articles/{id} (activo y verificado)
    alt Sin fuentes relevantes
        N->>BE: POST /responses sin fuentes → NO_RESULTS
    else Hay fuentes
        N->>L: Respuesta con contexto de las fuentes
        N->>BE: POST /responses con fuentes y similitud → COMPLETED
    end
    opt Falla embeddings, Qdrant o LLM
        N->>BE: POST /queries/{id}/failure → FAILED
    end
    BE->>DB: Guarda AiResponse, ResponseSource y estado
    FE-->>U: Diagnóstico, solución, pasos y fuentes
    U->>FE: Califica la respuesta (1 a 5)
    FE->>BE: POST /responses/{id}/ratings
```

## 4. Indexación de la base de conocimiento (RF03)

```mermaid
flowchart TD
    A[Webhook index-knowledge-base] --> B[Validar solicitud]
    B --> C["GET /api/v1/knowledge-articles (uno o todos)"]
    C --> D{¿Activo y verificado?}
    D -- No --> E[Eliminar su vector en Qdrant]
    D -- Sí --> F["Embeddings en lote (OpenRouter)"]
    F --> G[Upsert de puntos en Qdrant con metadatos]
    E --> H[Resumen de la indexación]
    G --> H
```
