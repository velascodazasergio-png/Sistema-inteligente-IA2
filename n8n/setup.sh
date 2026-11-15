#!/bin/sh
# Configura n8n antes de que arranque (ver el servicio n8n en docker-compose.yml):
#   1. crea o actualiza las credenciales "Webhook entrante (Spring Boot)" y "Qdrant API Key" con los valores del .env;
#   2. importa n8n/workflows/03-flujos-combinados.json con esas credenciales ya asignadas;
#   3. desactiva cualquier otro flujo (dos webhooks con la misma ruta impiden activar) y activa el combinado.
# Lo ejecuta el contenedor n8n en cada arranque, antes de "n8n start": el repositorio es la fuente de verdad de los flujos.
# Para conservar cambios hechos a mano en la interfaz de n8n: N8N_AUTO_SETUP=false en el .env.
set -eu

if [ "${N8N_AUTO_SETUP:-true}" = "false" ]; then
  echo "[n8n-setup] N8N_AUTO_SETUP=false: no se modifica n8n."
  exit 0
fi

if [ -z "${N8N_WEBHOOK_AUTH_VALUE:-}" ]; then
  echo "[n8n-setup] ERROR: falta N8N_WEBHOOK_AUTH_VALUE en el .env."
  echo "[n8n-setup] Es el token con el que el backend llama a n8n. Copia .env.example como .env y complétalo."
  exit 1
fi

WORKFLOW_FILE=/workflows/03-flujos-combinados.json
WORKFLOW_ID=flujoCombinado001
TMP=$(mktemp -d)

# Los JSON se arman con Node (viene en la imagen de n8n) para no romperlos con caracteres especiales
node - "$WORKFLOW_FILE" "$TMP" "$WORKFLOW_ID" <<'NODE'
const fs = require('fs');
const [file, out, workflowId] = process.argv.slice(2);
const env = process.env;

const credentials = [
  {
    id: 'credWebhookSpring',
    name: 'Webhook entrante (Spring Boot)',
    type: 'httpHeaderAuth',
    data: { name: env.N8N_WEBHOOK_AUTH_HEADER || 'X-N8N-Token', value: env.N8N_WEBHOOK_AUTH_VALUE },
  },
  {
    id: 'credQdrantApiKey',
    name: 'Qdrant API Key',
    type: 'httpHeaderAuth',
    // Qdrant no tiene clave en docker-compose; el valor solo debe existir
    data: { name: 'api-key', value: env.QDRANT_API_KEY || 'sin-clave' },
  },
];
fs.writeFileSync(`${out}/credentials.json`, JSON.stringify(credentials));

const idByName = Object.fromEntries(credentials.map((c) => [c.name, c.id]));
const workflow = JSON.parse(fs.readFileSync(file, 'utf8'));
workflow.id = workflowId;
for (const node of workflow.nodes) {
  for (const ref of Object.values(node.credentials || {})) {
    if (!idByName[ref.name]) throw new Error(`Credencial desconocida en el nodo "${node.name}": ${ref.name}`);
    ref.id = idByName[ref.name];
  }
}
fs.writeFileSync(`${out}/workflows.json`, JSON.stringify([workflow]));
NODE

echo "[n8n-setup] Credenciales..."
n8n import:credentials --input="$TMP/credentials.json"

echo "[n8n-setup] Flujo $WORKFLOW_FILE..."
n8n import:workflow --input="$TMP/workflows.json"

echo "[n8n-setup] Activando solo el flujo combinado..."
n8n update:workflow --all --active=false
n8n update:workflow --id="$WORKFLOW_ID" --active=true

rm -rf "$TMP"
echo "[n8n-setup] Listo."
