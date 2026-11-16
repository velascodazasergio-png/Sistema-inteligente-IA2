// Translates into Spanish the messages returned by the backend (DTO validations,
// business rules and missing resources). Unknown messages return null and
// the HTTP client falls back to a generic message for the status code.

const EXACT = {
  // Form validations (DTO)
  'Title is required': 'El título es obligatorio.',
  'Title must be between 5 and 150 characters': 'El título debe tener entre 5 y 150 caracteres.',
  'Title must be between 5 and 180 characters': 'El título debe tener entre 5 y 180 caracteres.',
  'Description is required': 'La descripción es obligatoria.',
  'Description must be between 20 and 4000 characters': 'La descripción debe tener entre 20 y 4000 caracteres.',
  'Detailed description is required': 'La descripción del incidente es obligatoria.',
  'Detailed description must contain at least 20 characters': 'La descripción del incidente debe tener al menos 20 caracteres.',
  'Additional context cannot exceed 500 characters': 'El contexto adicional no puede superar 500 caracteres.',
  'Priority is required': 'Elige una prioridad.',
  'User ID is required': 'Elige el usuario responsable.',
  'Category ID is required': 'Elige una categoría.',
  'Category name is required': 'Falta la categoría del incidente.',
  'Incident ID is required': 'Elige un incidente.',
  'Incident title cannot be blank': 'El incidente no tiene título.',
  'New status is required': 'Elige el nuevo estado.',
  'Problem is required': 'Describe el problema.',
  'Problem must be between 10 and 4000 characters': 'El problema debe tener entre 10 y 4000 caracteres.',
  'Symptoms cannot exceed 4000 characters': 'Los síntomas no pueden superar 4000 caracteres.',
  'Cause cannot exceed 4000 characters': 'La causa no puede superar 4000 caracteres.',
  'Solution is required': 'Describe la solución.',
  'Solution must be between 10 and 8000 characters': 'La solución debe tener entre 10 y 8000 caracteres.',
  'Reference URL cannot exceed 500 characters': 'La URL de referencia no puede superar 500 caracteres.',
  'Reference URL must start with http:// or https://': 'La URL de referencia debe empezar por http:// o https://.',
  'Evaluator user ID is required': 'Elige quién califica.',
  'Score is required': 'Elige de 1 a 5 estrellas.',
  'Minimum score is 1': 'La calificación mínima es 1 estrella.',
  'Maximum score is 5': 'La calificación máxima es 5 estrellas.',
  'Helpful indicator is required': 'Indica si la respuesta fue útil.',
  'Feedback comment cannot exceed 500 characters': 'El comentario no puede superar 500 caracteres.',
  // Session and permissions (401 / 403)
  'Invalid email or password': 'Correo o contraseña incorrectos.',
  'Authentication required': 'Inicia sesión para continuar.',
  'Invalid or expired token': 'Tu sesión expiró. Inicia sesión de nuevo.',
  'You do not have permission to perform this action': 'Tu rol no tiene permiso para realizar esta acción.',
  'You can only access your own incidents': 'Solo puedes consultar los incidentes que tú reportaste.',
  'This article is not published': 'Este artículo aún no está publicado.',
  'Email is required': 'Escribe tu correo.',
  'Email must be a valid address': 'Escribe un correo válido.',
  'Password is required': 'Escribe tu contraseña.',
  // Business rules (409)
  'Cannot change the status of an already CLOSED incident': 'El incidente está cerrado: ya no se puede cambiar su estado.',
  'Cannot modify an already CLOSED incident': 'El incidente está cerrado: ya no se puede modificar.',
  'Cannot request AI assistance for a CLOSED incident': 'No se puede consultar a la IA sobre un incidente cerrado.',
  'Send at least one field to update: active or verified': 'Indica qué quieres cambiar: activo o verificado.',
  'Start date cannot be after end date': 'La fecha inicial no puede ser posterior a la final.',
  'This AI response has already been rated': 'Esta respuesta ya fue calificada.',
  // Format errors (400)
  'Malformed request body or invalid field value': 'La solicitud tiene un formato inválido o un valor no permitido.',
};

const PATTERNS = [
  [/^Incident not found with ID: (\d+)/, (id) => `El incidente #${id} no existe.`],
  [/^Category not found with ID: (\d+)/, (id) => `La categoría #${id} no existe.`],
  [/^User not found with ID: (\d+)/, (id) => `El usuario #${id} no existe.`],
  [/^Evaluator user not found with ID: (\d+)/, (id) => `El usuario evaluador #${id} no existe.`],
  [/^Knowledge article not found with ID: (\d+)/, (id) => `El artículo #${id} no existe.`],
  [/^AI Query not found with ID: (\d+)/, (id) => `La consulta #${id} no existe.`],
  [/^AI Response not found with ID: (\d+)/, (id) => `La respuesta #${id} no existe.`],
  [/^Cannot (?:register an incident under|assign an incident to|assign an article to) an inactive category: (.+)/,
    (name) => `La categoría "${name}" está inactiva: elige otra.`],
  [/^Invalid value '(.*)' for parameter '(.+)'/, (value, name) => `El valor "${value}" no es válido para "${name}".`],
  [/^Invalid value '(.*)'/, (value) => `El valor "${value}" no es válido.`],
];

// Hints that the backend appends to the failure message of an AI query
// (backend/.../service/AiFailureHints.java). They come in English, possibly after the n8n message.
const SERVICE_NAMES = {
  'OpenRouter (embedding model)': 'OpenRouter (modelo de embeddings)',
  'OpenRouter (chat model)': 'OpenRouter (modelo de chat)',
};
// Spanish name of the service mentioned in a hint
const service = (name) => SERVICE_NAMES[name] ?? name;
const SERVICE = '(Qdrant|OpenRouter \\((?:embedding|chat) model\\))';

const FAILURE_HINTS = [
  [/The embedding model dimension does not match EMBEDDING_DIM: check the \.env file\./,
    () => 'La dimensión del modelo de embeddings no coincide con EMBEDDING_DIM: revisa el .env.'],
  [/An n8n configuration variable is missing \(EMBEDDING_MODEL, LLM_MODEL or RAG_SCORE_THRESHOLD\): check the \.env file\./,
    () => 'Falta una variable de configuración de n8n (EMBEDDING_MODEL, LLM_MODEL o RAG_SCORE_THRESHOLD): revisa el .env.'],
  [new RegExp(`${SERVICE} rejected the API key: check OPENROUTER_API_KEY in the \\.env file and apply the change with "docker compose up -d n8n"\\.`),
    (s) => `${service(s)} rechazó la clave de API: revisa OPENROUTER_API_KEY en el .env y aplica el cambio con "docker compose up -d n8n".`],
  [/The OpenRouter account does not have enough credits\./,
    () => 'La cuenta de OpenRouter no tiene créditos suficientes.'],
  [new RegExp(`${SERVICE} rate-limited the requests \\(429\\): wait a moment and try again\\.`),
    (s) => `${service(s)} limitó las solicitudes (429): espera un momento y vuelve a intentarlo.`],
  [new RegExp(`${SERVICE} does not recognize the configured model: check EMBEDDING_MODEL or LLM_MODEL in the \\.env file\\.`),
    (s) => `${service(s)} no reconoce el modelo configurado: revisa EMBEDDING_MODEL o LLM_MODEL en el .env.`],
  [/The Qdrant collection does not exist: run the initial indexing of workflow 01\./,
    () => 'No existe la colección en Qdrant: ejecuta la indexación inicial del flujo 01.'],
  [new RegExp(`${SERVICE} took too long to respond: try again\\.`),
    (s) => `${service(s)} tardó demasiado en responder: vuelve a intentarlo.`],
  [new RegExp(`The connection between n8n and ${SERVICE} was cut: try again\\.`),
    (s) => `Se cortó la conexión entre n8n y ${service(s)}: vuelve a intentarlo.`],
  [new RegExp(`n8n could not connect to ${SERVICE}: check the network or the configured URL\\.`),
    (s) => `n8n no pudo conectarse con ${service(s)}: revisa la red o la URL configurada.`],
];

/** Translates the English hint that the backend appends to the failure message of an AI query. */
export function translateFailureMessage(message) {
  if (typeof message !== 'string') return message;
  for (const [regex, build] of FAILURE_HINTS) {
    const match = regex.exec(message);
    if (match) return message.replace(regex, build(...match.slice(1)));
  }
  return message;
}

// Spanish text for a backend message, or null if it is not recognized
export function translateApiMessage(message) {
  if (typeof message !== 'string' || !message.trim()) return null;
  const text = message.trim();
  if (EXACT[text]) return EXACT[text];
  for (const [regex, build] of PATTERNS) {
    const match = regex.exec(text);
    if (match) return build(...match.slice(1));
  }
  return null;
}
