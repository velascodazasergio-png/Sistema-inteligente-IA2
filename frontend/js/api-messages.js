// Traduce al español los mensajes que devuelve el backend (validaciones de los DTO,
// reglas de negocio y recursos inexistentes). Lo que no se reconoce devuelve null y
// el cliente HTTP usa un mensaje genérico según el código de estado.

const EXACT = {
  // Validaciones de los formularios (DTO)
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
  // Reglas de negocio (409)
  'Cannot change the status of an already CLOSED incident': 'El incidente está cerrado: ya no se puede cambiar su estado.',
  'Cannot modify an already CLOSED incident': 'El incidente está cerrado: ya no se puede modificar.',
  'Cannot request AI assistance for a CLOSED incident': 'No se puede consultar a la IA sobre un incidente cerrado.',
  'Send at least one field to update: active or verified': 'Indica qué quieres cambiar: activo o verificado.',
  'Start date cannot be after end date': 'La fecha inicial no puede ser posterior a la final.',
  'This AI response has already been rated': 'Esta respuesta ya fue calificada.',
  // Errores de formato (400)
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
