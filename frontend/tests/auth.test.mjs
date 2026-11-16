// Tests for the session (JWT) and for the sections each role can see.
// Run from the repository root:  node --test "frontend/tests/*.test.mjs"

import { test, describe, beforeEach, afterEach } from 'node:test';
import assert from 'node:assert/strict';

import { isExpired, canAccess, homeFor, setSession, clearSession, getSession, currentUser, isEmployee, tokenExpiration } from '../js/auth.js';
import { logout } from '../js/services/auth.service.js';
import { routes, resolveRoute, visibleRoutes } from '../js/routes.js';
import { request } from '../js/api.js';

// Expiration date one hour from now
const inOneHour = () => new Date(Date.now() + 3600_000).toISOString();
// Route definition by path
const route = (path) => routes.find((r) => r.path === path);
// Sections a role can open
const pathsFor = (role) => routes.filter((r) => !r.public && canAccess(r, role)).map((r) => r.path);

describe('sesión', () => {
  afterEach(() => clearSession({ silent: true }));

  test('una sesión vencida o sin token no es válida', () => {
    assert.equal(isExpired({ token: 't', expiresAt: inOneHour() }), false);
    assert.equal(isExpired({ token: 't', expiresAt: '2020-01-01T00:00:00Z' }), true);
    assert.equal(isExpired({ token: '', expiresAt: inOneHour() }), true);
    assert.equal(isExpired({ token: 't', expiresAt: 'no es fecha' }), true);
    assert.equal(isExpired(null), true);
  });

  test('guarda y borra la sesión', () => {
    setSession({ accessToken: 'abc', expiresAt: inOneHour(), user: { id: 5, fullName: 'Laura Chen', role: 'EMPLOYEE' } });
    assert.equal(getSession().token, 'abc');
    assert.equal(currentUser().id, 5);
    assert.equal(isEmployee(), true);
    clearSession({ silent: true });
    assert.equal(getSession(), null);
  });

  test('getSession descarta una sesión ya vencida', () => {
    setSession({ accessToken: 'old', expiresAt: '2020-01-01T00:00:00Z', user: { id: 1, role: 'ADMIN' } });
    assert.equal(getSession(), null);
  });
});

describe('secciones por rol', () => {
  test('el administrador ve todas las secciones', () => {
    assert.deepEqual(pathsFor('ADMIN'),
      ['dashboard', 'incidentes', 'conocimiento', 'asistente', 'historial', 'analitica', 'configuracion', 'componentes']);
  });

  test('el técnico no ve configuración ni el catálogo de componentes', () => {
    assert.deepEqual(pathsFor('SUPPORT_TECHNICIAN'),
      ['dashboard', 'incidentes', 'conocimiento', 'asistente', 'historial', 'analitica']);
  });

  test('el usuario solo ve sus incidentes, la base de conocimiento, el asistente y su historial', () => {
    assert.deepEqual(pathsFor('EMPLOYEE'), ['incidentes', 'conocimiento', 'asistente', 'historial']);
  });

  test('sin sesión solo se puede abrir el login', () => {
    assert.equal(canAccess(route('login'), undefined), true);
    assert.equal(canAccess(route('incidentes'), undefined), false);
  });

  test('cada rol tiene su sección de inicio, y puede abrirla', () => {
    for (const role of ['ADMIN', 'SUPPORT_TECHNICIAN', 'EMPLOYEE']) {
      assert.equal(canAccess(route(homeFor(role)), role), true, role);
    }
    assert.equal(homeFor('EMPLOYEE'), 'incidentes');
    assert.equal(homeFor(undefined), 'login');
  });

  test('el usuario ve "Mis incidentes" y "Mis consultas"', () => {
    assert.equal(resolveRoute(route('incidentes'), 'EMPLOYEE').title, 'Mis incidentes');
    assert.equal(resolveRoute(route('historial'), 'EMPLOYEE').navLabel, 'Mis consultas');
    assert.equal(resolveRoute(route('incidentes'), 'ADMIN').title, 'Incidentes');
  });

  test('visibleRoutes usa el rol de la sesión', () => {
    setSession({ accessToken: 't', expiresAt: inOneHour(), user: { id: 2, role: 'SUPPORT_TECHNICIAN' } });
    assert.equal(visibleRoutes().some((r) => r.path === 'configuracion'), false);
    assert.equal(visibleRoutes().some((r) => r.path === 'login'), false);
    clearSession({ silent: true });
  });
});

describe('cliente HTTP con sesión', () => {
  const realFetch = globalThis.fetch;
  let calls;
  beforeEach(() => { calls = []; });
  afterEach(() => {
    globalThis.fetch = realFetch;
    clearSession({ silent: true });
  });

  // Fake fetch that records the call and answers with the given status and JSON
  const reply = (status, body) => async (url, init) => {
    calls.push({ url: String(url), init });
    return new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } });
  };

  test('envía el JWT en Authorization', async () => {
    setSession({ accessToken: 'jwt-123', expiresAt: inOneHour(), user: { id: 1, role: 'ADMIN' } });
    globalThis.fetch = reply(200, []);
    await request('/api/v1/incidents');
    assert.equal(calls[0].init.headers.Authorization, 'Bearer jwt-123');
  });

  test('sin sesión no envía Authorization', async () => {
    globalThis.fetch = reply(200, []);
    await request('/api/v1/incidents');
    assert.equal(calls[0].init.headers.Authorization, undefined);
  });

  test('un 401 con sesión la cierra y explica el motivo en español', async () => {
    setSession({ accessToken: 'vencido', expiresAt: inOneHour(), user: { id: 1, role: 'ADMIN' } });
    globalThis.fetch = reply(401, { status: 401, message: 'Invalid or expired token' });
    await assert.rejects(request('/api/v1/incidents'), (error) => error.status === 401 && /sesión expiró/.test(error.message));
    assert.equal(getSession(), null);
  });

  test('un 403 traduce el motivo y conserva la sesión', async () => {
    setSession({ accessToken: 'ok', expiresAt: inOneHour(), user: { id: 5, role: 'EMPLOYEE' } });
    globalThis.fetch = reply(403, { status: 403, message: 'You can only access your own incidents' });
    await assert.rejects(request('/api/v1/incidents/2'), (error) => error.status === 403 && /que tú reportaste/.test(error.message));
    assert.notEqual(getSession(), null);
  });

  test('credenciales incorrectas: mensaje en español', async () => {
    globalThis.fetch = reply(401, { status: 401, message: 'Invalid email or password' });
    await assert.rejects(request('/api/v1/auth/login', { method: 'POST', body: {}, auth: false }),
      (error) => error.message === 'Correo o contraseña incorrectos.');
  });
});

describe('la sesión vive en la base de datos', () => {
  const realFetch = globalThis.fetch;
  const realStorage = globalThis.localStorage;
  let store;
  beforeEach(() => {
    store = new Map();
    // localStorage simulado: permite ver exactamente qué guarda el navegador
    globalThis.localStorage = {
      getItem: (k) => (store.has(k) ? store.get(k) : null),
      setItem: (k, v) => store.set(k, String(v)),
      removeItem: (k) => store.delete(k),
    };
  });
  afterEach(() => {
    globalThis.fetch = realFetch;
    globalThis.localStorage = realStorage;
    clearSession({ silent: true });
  });

  test('el navegador solo guarda el token, no los datos del usuario', () => {
    setSession({ accessToken: 'jwt-abc', expiresAt: inOneHour(), user: { id: 5, fullName: 'Laura Chen', role: 'EMPLOYEE' } });
    assert.deepEqual([...store.entries()], [['incidentes-ia:token', 'jwt-abc']]);
  });

  test('la fecha de vencimiento se lee del propio token (claim exp)', () => {
    const payload = Buffer.from(JSON.stringify({ exp: 1893456000 })).toString('base64url');
    assert.equal(tokenExpiration(`h.${payload}.s`), '2030-01-01T00:00:00.000Z');
    assert.equal(tokenExpiration('no-es-un-jwt'), null);
  });

  test('cerrar sesión la cierra en el servidor y borra el token', async () => {
    const calls = [];
    globalThis.fetch = async (url, init) => {
      calls.push({ url: String(url), method: init.method });
      return new Response(null, { status: 204 });
    };
    setSession({ accessToken: 'jwt-abc', expiresAt: inOneHour(), user: { id: 5, role: 'EMPLOYEE' } });
    await logout();
    assert.equal(calls[0].method, 'POST');
    assert.match(calls[0].url, /\/api\/v1\/auth\/logout$/);
    assert.equal(store.size, 0);
    assert.equal(getSession(), null);
  });
});

