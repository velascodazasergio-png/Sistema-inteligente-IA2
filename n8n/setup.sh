#!/bin/sh
# Configures n8n before it starts (see the n8n service in docker-compose.yml):
#   1. creates or updates the "Webhook entrante (Spring Boot)" and "Qdrant API Key" credentials with the .env values;
#   2. imports n8n/workflows/03-flujos-combinados.json with those credentials already assigned;
#   3. deactivates any other workflow (two webhooks with the same path block activation) and activates the combined one.
# The n8n container runs it on every start, before "n8n start": the repository is the source of truth for the workflows.
# To keep changes made by hand in the n8n editor: N8N_AUTO_SETUP=false in the .env file.
set -eu

if [ "${N8N_AUTO_SETUP:-true}" = "false" ]; then
  echo "[n8n-setup] N8N_AUTO_SETUP=false: n8n is left unchanged."
  exit 0
fi

if [ -z "${N8N_WEBHOOK_AUTH_VALUE:-}" ]; then
  echo "[n8n-setup] ERROR: N8N_WEBHOOK_AUTH_VALUE is missing in the .env file."
  echo "[n8n-setup] It is the token the backend uses to call n8n. Copy .env.example to .env and fill it in."
  exit 1
fi

WORKFLOW_FILE=/workflows/03-flujos-combinados.json
WORKFLOW_ID=flujoCombinado001
TMP=$(mktemp -d)

# The JSON files are built with Node (included in the n8n image) so special characters cannot break them
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
    // Qdrant has no key in docker-compose; the value only has to exist
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

echo "[n8n-setup] Credentials..."
n8n import:credentials --input="$TMP/credentials.json"

echo "[n8n-setup] Workflow $WORKFLOW_FILE..."
n8n import:workflow --input="$TMP/workflows.json"

echo "[n8n-setup] Activating only the combined workflow..."
n8n update:workflow --all --active=false
n8n update:workflow --id="$WORKFLOW_ID" --active=true

rm -rf "$TMP"
echo "[n8n-setup] Done."
