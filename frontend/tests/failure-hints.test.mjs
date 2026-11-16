// Las indicaciones de fallo de la IA llegan del backend en inglés (AiFailureHints.java)
// y la interfaz las muestra en español. Si cambia un texto en el backend, esta prueba falla.
// Ejecutar desde la raíz del repositorio:  node --test "frontend/tests/*.test.mjs"

import { test, describe } from 'node:test';
import assert from 'node:assert/strict';

import { translateFailureMessage } from '../js/api-messages.js';

const N8N = 'No se pudo generar el embedding de la consulta.';

// Texto exacto que devuelve AiFailureHints.hint(...) en cada caso → traducción esperada
const CASES = [
  ['The embedding model dimension does not match EMBEDDING_DIM: check the .env file.',
    'La dimensión del modelo de embeddings no coincide con EMBEDDING_DIM: revisa el .env.'],
  ['An n8n configuration variable is missing (EMBEDDING_MODEL, LLM_MODEL or RAG_SCORE_THRESHOLD): check the .env file.',
    'Falta una variable de configuración de n8n (EMBEDDING_MODEL, LLM_MODEL o RAG_SCORE_THRESHOLD): revisa el .env.'],
  ['OpenRouter (embedding model) rejected the API key: check OPENROUTER_API_KEY in the .env file and apply the change with "docker compose up -d n8n".',
    'OpenRouter (modelo de embeddings) rechazó la clave de API: revisa OPENROUTER_API_KEY en el .env y aplica el cambio con "docker compose up -d n8n".'],
  ['The OpenRouter account does not have enough credits.',
    'La cuenta de OpenRouter no tiene créditos suficientes.'],
  ['OpenRouter (chat model) rate-limited the requests (429): wait a moment and try again.',
    'OpenRouter (modelo de chat) limitó las solicitudes (429): espera un momento y vuelve a intentarlo.'],
  ['OpenRouter (chat model) does not recognize the configured model: check EMBEDDING_MODEL or LLM_MODEL in the .env file.',
    'OpenRouter (modelo de chat) no reconoce el modelo configurado: revisa EMBEDDING_MODEL o LLM_MODEL en el .env.'],
  ['The Qdrant collection does not exist: run the initial indexing of workflow 01.',
    'No existe la colección en Qdrant: ejecuta la indexación inicial del flujo 01.'],
  ['Qdrant took too long to respond: try again.',
    'Qdrant tardó demasiado en responder: vuelve a intentarlo.'],
  ['The connection between n8n and Qdrant was cut: try again.',
    'Se cortó la conexión entre n8n y Qdrant: vuelve a intentarlo.'],
  ['n8n could not connect to OpenRouter (embedding model): check the network or the configured URL.',
    'n8n no pudo conectarse con OpenRouter (modelo de embeddings): revisa la red o la URL configurada.'],
];

describe('indicaciones de fallo de la IA', () => {
  test('cada indicación del backend se muestra en español, después del mensaje de n8n', () => {
    for (const [english, spanish] of CASES) {
      assert.equal(translateFailureMessage(`${N8N} ${english}`), `${N8N} ${spanish}`, english);
    }
  });

  test('un mensaje sin indicación, o ya en español (consultas antiguas), no cambia', () => {
    assert.equal(translateFailureMessage(N8N), N8N);
    const old = `${N8N} OpenRouter (modelo de embeddings) rechazó la clave de API: revisa OPENROUTER_API_KEY en el .env.`;
    assert.equal(translateFailureMessage(old), old);
    assert.equal(translateFailureMessage(null), null);
  });
});
