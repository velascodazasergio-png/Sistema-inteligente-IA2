# Evidencias

| Archivo | Contenido |
|---|---|
| [`swagger-ui.png`](swagger-ui.png) | Swagger UI con los 26 endpoints documentados, agrupados por módulo, y el botón Authorize para el JWT |
| [`openapi.json`](openapi.json) | Especificación OpenAPI 3.1 exportada de `/api-docs` |
| [`postman_collection.json`](postman_collection.json) | Colección Postman (25 peticiones con pruebas del código HTTP esperado). Importar en Postman y ejecutar en orden |
| [`ejecucion-api.md`](ejecucion-api.md) | Resultado de ejecutar esas 25 peticiones contra el backend: 25/25 |
| [`frontend/`](frontend) | Capturas de cada módulo y de cada rol (administrador, técnico y usuario) con datos reales, en escritorio y móvil. Se muestran en el README principal |

Para regenerar la especificación: con el backend en marcha, `curl http://localhost:8080/api-docs`.
