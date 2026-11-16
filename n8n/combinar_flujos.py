"""Builds n8n/workflows/03-flujos-combinados.json from workflows 01 and 02.

The result is ONE workflow with two independent chains (two webhooks, no connections between them):
- "01 · …": knowledge base indexing (webhook index-knowledge-base)
- "02 · …": RAG assistance (webhook analyze-incident)

Workflows 01 and 02 remain the source of truth: if they change, run this script again.
Usage (from the repository root):  python3 n8n/combinar_flujos.py
"""
import copy
import json
import re
import uuid
from pathlib import Path

BASE = Path(__file__).resolve().parent / 'workflows'
SOURCES = [('01', '01-indexacion-base-conocimiento.json'), ('02', '02-asistencia-rag.json')]
OUTPUT = BASE / '03-flujos-combinados.json'
GAP_Y = 400  # vertical gap between the two chains on the canvas


def rename_refs(value, mapping):
    """Updates $('Node name') references in code and expressions."""
    if isinstance(value, str):
        return re.sub(r"\$\('([^']+)'\)", lambda m: f"$('{mapping.get(m.group(1), m.group(1))}')", value)
    if isinstance(value, list):
        return [rename_refs(v, mapping) for v in value]
    if isinstance(value, dict):
        return {k: rename_refs(v, mapping) for k, v in value.items()}
    return value


def prefixed(prefix, workflow, offset_y):
    """Copies a workflow with "<prefix> · " in every node name, new ids and its nodes moved down by offset_y."""
    mapping = {n['name']: f'{prefix} · {n["name"]}' for n in workflow['nodes']}
    nodes = []
    for node in workflow['nodes']:
        n = copy.deepcopy(node)
        n['name'] = mapping[node['name']]
        n['id'] = str(uuid.uuid5(uuid.NAMESPACE_URL, f'incidentes-ia/{prefix}/{node["id"]}'))
        n['position'] = [node['position'][0], node['position'][1] + offset_y]
        n['parameters'] = rename_refs(n['parameters'], mapping)
        nodes.append(n)
    connections = {}
    for source, outputs in workflow['connections'].items():
        connections[mapping[source]] = {
            kind: [[{**link, 'node': mapping[link['node']]} for link in branch] for branch in branches]
            for kind, branches in outputs.items()
        }
    return nodes, connections


def main():
    """Merges both workflows, adds an explanatory sticky note and writes the combined JSON."""
    flows = [(p, json.loads((BASE / f).read_text(encoding='utf-8'))) for p, f in SOURCES]
    names = [n['name'] for _, w in flows for n in w['nodes']]
    assert len(names) == sum(len(w['nodes']) for _, w in flows)

    all_nodes, all_connections, offset = [], {}, 120
    for prefix, workflow in flows:
        min_y = min(n['position'][1] for n in workflow['nodes'])
        nodes, connections = prefixed(prefix, workflow, offset - min_y)
        all_nodes += nodes
        all_connections.update(connections)
        offset = max(n['position'][1] for n in nodes) + GAP_Y

    all_nodes.insert(0, {
        'parameters': {
            'content': '## Flujos 01 + 02 combinados\n'
                       'Dos cadenas **independientes** en un solo workflow: cada webhook ejecuta solo la suya.\n'
                       '- **Arriba (01 ·):** indexación en Qdrant, webhook `index-knowledge-base` (RF03).\n'
                       '- **Abajo (02 ·):** asistencia RAG, webhook `analyze-incident` (RF04, RF05).\n\n'
                       'Usa **este** workflow **o** los flujos 01 y 02 por separado, no ambos activos: '
                       'n8n no permite dos webhooks activos con la misma ruta.\n'
                       'Generado con `python3 n8n/combinar_flujos.py`.',
            'height': 220, 'width': 760, 'color': 4,
        },
        'id': str(uuid.uuid5(uuid.NAMESPACE_URL, 'incidentes-ia/combinado/nota')),
        'name': 'Acerca de este workflow',
        'type': 'n8n-nodes-base.stickyNote',
        'typeVersion': 1,
        'position': [40, -160],
    })

    combined = {
        'name': 'Flujos 01 + 02 combinados (indexación y asistencia RAG)',
        'nodes': all_nodes,
        'connections': all_connections,
        'active': False,
        'settings': {'executionOrder': 'v1'},
        'pinData': {},
    }
    OUTPUT.write_text(json.dumps(combined, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(f'{OUTPUT.name}: {len(all_nodes)} nodes, {len(all_connections)} connections')


if __name__ == '__main__':
    main()
