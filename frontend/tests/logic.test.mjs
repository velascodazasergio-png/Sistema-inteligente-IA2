// Pruebas unitarias de la lógica del frontend (sin navegador ni dependencias).
// Ejecutar desde la raíz del repositorio:  node --test "frontend/tests/*.test.mjs"
// Cubren cálculos, formato, interpretación de respuestas de la IA, catálogos y el cliente HTTP.

import { test, describe, beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';

import { formatYearWeek, formatRatio, countBy, parseDate } from '../js/format.js';
import { distributions, previousPeriod, pctChange, formatYearMonth, TIME_BUCKETS } from '../js/analytics-utils.js';
import { parseResponse, responseSummary } from '../js/components/ai-response.js';
import { estadoConsulta, badge, ESTADO_CONSULTA, labelOf, PRIORIDAD } from '../js/catalogs.js';
import { escapeHtml } from '../js/ui.js';
import { request, ApiError } from '../js/api.js';
import { requestAssistance, MIN_DESCRIPTION } from '../js/services/ai-assistance.service.js';

describe('format.js', () => {
  test('formatYearWeek convierte la semana ISO del backend', () => {
    assert.equal(formatYearWeek('2026-W07'), 'Sem 7');
    assert.equal(formatYearWeek(null), '—');
  });

  test('formatRatio muestra porcentaje y tolera valores ausentes', () => {
    assert.match(formatRatio(0.912), /91\s?%/);
    assert.equal(formatRatio(undefined), '—');
  });

  test('countBy agrupa y ordena de mayor a menor', () => {
    const result = countBy(['a', 'b', 'a', 'c', 'a', 'b'], (x) => x);
    assert.deepEqual(result, [{ key: 'a', count: 3 }, { key: 'b', count: 2 }, { key: 'c', count: 1 }]);
  });

  test('parseDate devuelve null con fechas inválidas', () => {
    assert.equal(parseDate('no-es-fecha'), null);
    assert.equal(parseDate('2026-11-08T15:19:00').getHours(), 15);
  });
});

describe('analytics-utils.js', () => {
  const incidents = [
    { status: 'RESOLVED', priority: 'HIGH', createdAt: '2026-11-01T08:00:00', updatedAt: '2026-11-01T10:30:00' }, // 2,5 h
    { status: 'CLOSED', priority: 'LOW', createdAt: '2026-11-02T08:00:00', updatedAt: '2026-11-02T08:30:00' }, // 0,5 h
    { status: 'OPEN', priority: 'CRITICAL', createdAt: '2026-11-03T08:00:00', updatedAt: '2026-11-03T08:00:00' },
    { status: 'IN_AI_ANALYSIS', priority: 'CRITICAL', createdAt: '2026-11-04T08:00:00', updatedAt: null },
  ];

  test('distributions cuenta pendientes, críticos abiertos y tiempos con las reglas del backend', () => {
    const d = distributions(incidents);
    assert.equal(d.pending, 2);
    assert.equal(d.critical, 2);
    assert.equal(d.byStatus.reduce((s, x) => s + x.value, 0), incidents.length);
    assert.equal(d.times.find((b) => b.label === '< 1 h').value, 1);
    assert.equal(d.times.find((b) => b.label === '1–4 h').value, 1);
    assert.equal(d.times.length, TIME_BUCKETS.length);
  });

  test('previousPeriod devuelve el periodo anterior de la misma duración', () => {
    assert.deepEqual(previousPeriod('2026-11-01', '2026-11-30'), { startDate: '2026-10-02', endDate: '2026-10-31', days: 30 });
    assert.equal(previousPeriod('2026-11-01', ''), null);
  });

  test('pctChange no divide entre cero', () => {
    assert.equal(Math.round(pctChange(69, 81) * 10) / 10, -14.8);
    assert.equal(pctChange(5, 0), null);
    assert.equal(pctChange(null, 3), null);
  });

  test('formatYearMonth muestra el mes en español', () => {
    assert.match(formatYearMonth('2026-10'), /oct/);
  });
});

describe('ai-response.js · parseResponse', () => {
  test('separa solución, explicación, pasos y advertencias del formato del flujo RAG', () => {
    const parts = parseResponse({
      suggestedDiagnosis: 'Solución sugerida: Limpiar credenciales.\n\nExplicación del problema: El perfil móvil usa una clave antigua.',
      solutionSteps: '1. Revisar el evento 4740.\n2. Limpiar el Administrador de credenciales.\n\nAdvertencias:\n- Probar primero en un equipo.',
    });
    assert.equal(parts.solution, 'Limpiar credenciales.');
    assert.equal(parts.explanation, 'El perfil móvil usa una clave antigua.');
    assert.deepEqual(parts.steps, ['Revisar el evento 4740.', 'Limpiar el Administrador de credenciales.']);
    assert.deepEqual(parts.warnings, ['Probar primero en un equipo.']);
  });

  test('separa pasos numerados en una sola línea sin cortar números internos', () => {
    const parts = parseResponse({ suggestedDiagnosis: 'x', solutionSteps: '1. Actualizar a la versión 2.3 hoy. 2. Reiniciar el servicio 4740.' });
    assert.deepEqual(parts.steps, ['Actualizar a la versión 2.3 hoy.', 'Reiniciar el servicio 4740.']);
  });

  test('una respuesta sin resultados solo trae advertencias', () => {
    const parts = parseResponse({
      suggestedDiagnosis: 'Sin información suficiente en la base de conocimiento verificada.',
      solutionSteps: 'Advertencias:\n- No se encontraron artículos sobre el umbral (0.75).',
    });
    assert.deepEqual(parts.steps, []);
    assert.equal(parts.warnings.length, 1);
    assert.equal(parts.solution, '');
  });

  test('un texto libre se muestra completo como diagnóstico', () => {
    const parts = parseResponse({ suggestedDiagnosis: 'Firewall bloquea UDP 4500.', solutionSteps: null });
    assert.equal(parts.explanation, 'Firewall bloquea UDP 4500.');
    assert.deepEqual(parts.steps, []);
  });

  test('responseSummary traduce el fallo de conexión y resume cada estado', () => {
    assert.match(responseSummary({ queryStatus: 'COMMUNICATION_ERROR', failureCode: 'N8N_UNREACHABLE', failureMessage: 'english' }), /n8n/);
    assert.equal(responseSummary({ queryStatus: 'PENDING' }), 'Esperando respuesta…');
    assert.equal(responseSummary({ queryStatus: 'FAILED', failureMessage: 'LLM caído' }), 'LLM caído');
    assert.equal(responseSummary({
      queryStatus: 'COMPLETED',
      aiResponse: { suggestedDiagnosis: 'Solución sugerida: Reiniciar.', sources: [{}] },
    }), 'Reiniciar.');
  });
});

describe('catalogs.js', () => {
  test('una consulta COMPLETED sin fuentes se muestra como Sin resultados', () => {
    assert.equal(estadoConsulta({ queryStatus: 'COMPLETED', aiResponse: { sources: [] } }), 'NO_RESULTS');
    assert.equal(estadoConsulta({ queryStatus: 'COMPLETED', aiResponse: { sources: [{}] } }), 'COMPLETED');
    assert.equal(estadoConsulta({ queryStatus: 'FAILED' }), 'FAILED');
  });

  test('cubre los cinco estados del backend', () => {
    assert.deepEqual(Object.keys(ESTADO_CONSULTA).sort(), ['COMMUNICATION_ERROR', 'COMPLETED', 'FAILED', 'NO_RESULTS', 'PENDING']);
    assert.equal(labelOf(PRIORIDAD, 'CRITICAL'), 'Crítica');
  });

  test('badge escapa valores desconocidos (evita inyectar HTML)', () => {
    const html = badge(ESTADO_CONSULTA, '<img src=x onerror=alert(1)>');
    assert.ok(!html.includes('<img'));
    assert.ok(html.includes('&lt;img'));
  });

  test('escapeHtml escapa los cinco caracteres especiales', () => {
    assert.equal(escapeHtml(`<a href="x">'&'</a>`), '&lt;a href=&quot;x&quot;&gt;&#39;&amp;&#39;&lt;/a&gt;');
  });
});

describe('api.js · cliente HTTP', () => {
  const realFetch = globalThis.fetch;
  let calls;

  const reply = (status, body) => async (url, init) => {
    calls.push({ url: String(url), init });
    return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
  };

  beforeEach(() => { calls = []; });
  afterEach(() => { globalThis.fetch = realFetch; });

  test('arma la URL con la base configurada, omite parámetros vacíos y devuelve el JSON', async () => {
    globalThis.fetch = reply(200, [{ id: 1 }]);
    const data = await request('/api/v1/incidents', { query: { priority: 'HIGH', status: undefined, categoryId: '' } });
    assert.deepEqual(data, [{ id: 1 }]);
    assert.equal(calls[0].url, 'http://localhost:8080/api/v1/incidents?priority=HIGH');
  });

  test('envía el cuerpo como JSON', async () => {
    globalThis.fetch = reply(201, { id: 9 });
    await request('/api/v1/incidents', { method: 'POST', body: { title: 'x' } });
    assert.equal(calls[0].init.method, 'POST');
    assert.equal(calls[0].init.headers['Content-Type'], 'application/json');
    assert.equal(calls[0].init.body, '{"title":"x"}');
  });

  test('convierte los errores de validación de Spring en un mensaje legible', async () => {
    globalThis.fetch = reply(400, { status: 400, details: { title: 'Title is required', description: 'Description is required' } });
    await assert.rejects(request('/api/v1/incidents', { method: 'POST', body: {} }), (error) => {
      assert.ok(error instanceof ApiError);
      assert.equal(error.status, 400);
      assert.match(error.message, /Title is required · Description is required/);
      return true;
    });
  });

  test('usa el mensaje de negocio del backend en un 409', async () => {
    globalThis.fetch = reply(409, { status: 409, message: 'Cannot change the status of an already CLOSED incident' });
    await assert.rejects(request('/x'), /already CLOSED/);
  });

  test('marca NETWORK_ERROR cuando no hay conexión', async () => {
    globalThis.fetch = async () => { throw new TypeError('Failed to fetch'); };
    await assert.rejects(request('/x'), (error) => error.code === 'NETWORK_ERROR');
  });

  test('requestAssistance valida la descripción antes de llamar al backend', async () => {
    globalThis.fetch = reply(202, {});
    await assert.rejects(requestAssistance({ id: 1, description: 'corta' }), (error) => error.code === 'VALIDATION');
    assert.equal(calls.length, 0);
    await requestAssistance({ id: 1, title: 't', description: 'x'.repeat(MIN_DESCRIPTION), categoryName: 'c', priority: 'LOW' });
    assert.equal(JSON.parse(calls[0].init.body).incidentId, 1);
  });
});
