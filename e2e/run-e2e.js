#!/usr/bin/env node

const crypto = require('node:crypto');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const root = path.resolve(__dirname, '..');
const flowelleDir = path.resolve(process.env.FLOWELLE_DIR || path.join(root, '..', 'Flowelle'));
const runtimeDir = path.join(__dirname, '.runtime');
const composeFile = path.join(__dirname, 'docker-compose.yml');
const projectName = `aif-e2e-${process.pid}`;
const callbackSecret = process.env.E2E_AIF_CALLBACK_SECRET || 'e2e-aif-callback-secret';
const callbackKeyId = process.env.E2E_AIF_CALLBACK_KEY_ID || 'e2e-v1';
const userContextKeyId = process.env.E2E_USER_CONTEXT_KEY_ID || 'flowelle-e2e-v1';
const model = process.env.E2E_OLLAMA_MODEL || 'qwen2.5:0.5b';
const tenantSlug = 'flowelle';
const aiFriendUrl = 'http://localhost:18080';
const authUrl = 'http://localhost:18081/api';
const cyclesUrl = 'http://localhost:18082';

let composeStarted = false;

function fail(message) {
  throw new Error(message);
}

function assert(condition, message) {
  if (!condition) fail(message);
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    cwd: options.cwd || root,
    encoding: 'utf8',
    stdio: options.capture === false ? 'inherit' : ['ignore', 'pipe', 'pipe'],
    maxBuffer: options.maxBuffer || 20 * 1024 * 1024,
    env: process.env,
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    fail(`${command} ${args.join(' ')} failed (${result.status}): ${result.stderr || result.stdout}`);
  }
  return (result.stdout || '').trim();
}

function compose(args, options = {}) {
  return run('docker', ['compose', '--project-name', projectName, '--env-file', path.join(runtimeDir, '.env'), '-f', composeFile, ...args], options);
}

function base64url(value) {
  return Buffer.from(value).toString('base64url');
}

function signJwt(privateKey, kid, claims) {
  const header = base64url(JSON.stringify({ alg: 'RS256', kid, typ: 'JWT' }));
  const payload = base64url(JSON.stringify(claims));
  const input = `${header}.${payload}`;
  const signature = crypto.createSign('RSA-SHA256');
  signature.update(input);
  signature.end();
  return `${input}.${signature.sign(privateKey).toString('base64url')}`;
}

function generateKeys() {
  const admin = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
  const userContext = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
  const publicJwk = admin.publicKey.export({ format: 'jwk' });
  publicJwk.kid = 'e2e-admin-v1';
  publicJwk.use = 'sig';
  publicJwk.alg = 'RS256';
  fs.writeFileSync(path.join(runtimeDir, 'admin-jwks.json'), JSON.stringify({ keys: [publicJwk] }));
  const privateKeyBase64 = userContext.privateKey.export({ type: 'pkcs8', format: 'der' }).toString('base64');
  return { admin, privateKeyBase64 };
}

async function request(url, options = {}) {
  const response = await fetch(url, { ...options, signal: AbortSignal.timeout(20000) });
  const text = await response.text();
  let body = text;
  try { body = text ? JSON.parse(text) : null; } catch (_) { /* preserve non-JSON diagnostics */ }
  return { response, body, text };
}

async function expectStatus(url, options, expected, label) {
  const result = await request(url, options);
  const statuses = Array.isArray(expected) ? expected : [expected];
  assert(statuses.includes(result.response.status), `${label}: expected ${statuses.join('/')} but received ${result.response.status}: ${result.text}`);
  return result.body;
}

async function waitFor(url, label) {
  const deadline = Date.now() + 180000;
  let lastError = 'no response';
  while (Date.now() < deadline) {
    try {
      const result = await request(url);
      if (result.response.status === 200 && result.body?.status === 'UP') return;
      lastError = `${result.response.status}: ${result.text}`;
    } catch (error) {
      lastError = error.message;
    }
    await new Promise(resolve => setTimeout(resolve, 2000));
  }
  fail(`${label} did not become ready: ${lastError}`);
}

function adminToken(adminKeys) {
  const now = Math.floor(Date.now() / 1000);
  return signJwt(adminKeys.privateKey, 'e2e-admin-v1', {
    iss: 'https://e2e-admin.local',
    sub: 'e2e-admin',
    aud: 'ai-friend-admin',
    iat: now,
    exp: now + 120,
    jti: crypto.randomUUID(),
    roles: ['aif-admin'],
  });
}

function hmac(timestamp, body) {
  return crypto.createHmac('sha256', callbackSecret).update(`${timestamp}.${body}`).digest('hex');
}

async function configureAiFriend(adminKeys) {
  const headers = {
    Authorization: `Bearer ${adminToken(adminKeys)}`,
    'Content-Type': 'application/json',
  };
  await expectStatus(`${aiFriendUrl}/internal/admin/tenants/${tenantSlug}`, {
    method: 'PUT', headers,
    body: JSON.stringify({ slug: tenantSlug, displayName: 'Flowelle E2E', active: true }),
  }, 200, 'configure Flowelle tenant');
  await expectStatus(`${aiFriendUrl}/internal/admin/tenants/${tenantSlug}/user-auth`, {
    method: 'PUT', headers,
    body: JSON.stringify({
      issuer: 'flowelle',
      audience: 'ai-friend-chat',
      jwksUri: 'http://flowelle-auth:8081/api/.well-known/jwks.json',
      allowedAlgorithm: 'RS256',
      maxTokenLifetimeSeconds: 120,
      jwksCacheTtlSeconds: 5,
      active: true,
    }),
  }, 200, 'configure user authentication');

  const tools = [
    ['cycle-summary', 'http://flowelle-cycles:8082/api/aif/tools/cycle-summary', ['cycle:read'], 'flowelle.cycle-summary.v1'],
    ['user-preferences', 'http://flowelle-auth:8081/api/aif/tools/user-preferences', ['preferences:read'], 'flowelle.user-preferences.v1'],
    ['nutrition-profile', 'http://flowelle-auth:8081/api/aif/tools/nutrition-profile', ['nutrition:read'], 'flowelle.nutrition-profile.v1'],
    ['exercise-profile', 'http://flowelle-auth:8081/api/aif/tools/exercise-profile', ['exercise:read'], 'flowelle.exercise-profile.v1'],
    ['recent-wellness-signals', 'http://flowelle-cycles:8082/api/aif/tools/recent-wellness-signals', ['signals:read'], 'flowelle.recent-wellness-signals.v1'],
  ];
  for (const [name, callbackUrl, scopes, contractVersion] of tools) {
    await expectStatus(`${aiFriendUrl}/internal/admin/tenants/${tenantSlug}/tools/${name}`, {
      method: 'PUT', headers,
      body: JSON.stringify({
        name,
        callbackUrl,
        secretRef: 'env://E2E_AIF_CALLBACK_SECRET',
        signingKeyId: callbackKeyId,
        contractVersion,
        allowedScopes: scopes,
        active: true,
      }),
    }, 200, `configure ${name}`);
  }

  const capabilities = [
    ['cycle-insights', 'Cycle insights', ['next period', 'cycle length', 'cycle stats'], ['cycle:read'], ['cycle-summary'], ['menstrual-cycle']],
    ['profile-preferences', 'Profile preferences', ['preferences', 'profile'], ['preferences:read'], ['user-preferences'], []],
    ['nutrition-guidance', 'Nutrition guidance', ['food', 'diet', 'nutrition', 'pms nutrition'], ['nutrition:read', 'cycle:read', 'signals:read'], ['nutrition-profile', 'cycle-summary', 'recent-wellness-signals'], ['pms-nutrition']],
    ['exercise-guidance', 'Exercise guidance', ['exercise', 'movement', 'workout'], ['exercise:read', 'cycle:read', 'signals:read'], ['exercise-profile', 'cycle-summary', 'recent-wellness-signals'], ['period-exercise']],
  ];
  for (const [key, displayName, triggerPhrases, requiredScopes, toolNames, retrievalTopics] of capabilities) {
    await expectStatus(`${aiFriendUrl}/internal/admin/tenants/${tenantSlug}/capabilities/${key}`, {
      method: 'PUT', headers,
      body: JSON.stringify({ displayName, description: `${displayName} for Flowelle users`, triggerPhrases, requiredScopes, toolNames, retrievalTopics, priority: 100, active: true }),
    }, 200, `configure ${key}`);
  }
}

function findToolCalls(body) {
  assert(Array.isArray(body?.toolCalls), `Expected toolCalls array: ${JSON.stringify(body)}`);
  return body.toolCalls;
}

function findTool(toolCalls, name) {
  return toolCalls.find(tool => tool.name === name);
}

async function registerUser() {
  const email = `e2e-${Date.now()}@example.com`;
  const password = 'E2e-password-123!';
  const body = await expectStatus(`${authUrl}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      firstName: 'E2E', lastName: 'Tester', email, password,
      cycleLength: 28, periodLength: 5, lastPeriodDate: '2026-09-01', birthControlUse: false,
    }),
  }, 200, 'register Flowelle user');
  assert(body?.token && body?.user?.id, `Registration did not return token and numeric user: ${JSON.stringify(body)}`);
  const user = { email, password, userId: String(body.user.id), token: body.token };
  await expectStatus(`${authUrl}/auth/me/wellness-profile`, {
    method: 'PUT', headers: { Authorization: `Bearer ${user.token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ dietaryPattern: 'VEGETARIAN', activityLevel: 'MODERATE', allergens: ['PEANUT'], intolerances: [], nutritionGoals: ['PMS_SUPPORT', 'HYDRATION'], preferredActivities: ['WALKING', 'YOGA'], exerciseGoals: ['GENTLE_MOVEMENT'], exerciseLimitations: [] }),
  }, 200, 'save wellness profile');
  return user;
}

async function setConsent(user, enabled) {
  await expectStatus(`${authUrl}/auth/me/privacy`, {
    method: 'PUT',
    headers: { Authorization: `Bearer ${user.token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ aiCoachEnabled: enabled, voiceProcessingEnabled: false, analyticsOptIn: false, notificationsEnabled: true }),
  }, 200, `set consent=${enabled}`);
  const body = await expectStatus(`${authUrl}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: user.email, password: user.password }),
  }, 200, `login after consent=${enabled}`);
  assert(body?.token, 'Login did not return a token');
  user.token = body.token;
}

async function chat(user, message) {
  return expectStatus(`${authUrl}/aif/chat/messages`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${user.token}`, 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId: null, message, locale: 'en-US' }),
  }, 200, 'Flowelle proxy chat');
}

async function testFullConsent(user) {
  await setConsent(user, true);
  const body = await chat(user, 'When is my next period and what food or exercise can help with PMS?');
  assert(typeof body.answer === 'string' && body.answer.length > 0, 'Consent-enabled answer is empty');
  const calls = findToolCalls(body);
  assert(findTool(calls, 'cycle-summary')?.status === 'COMPLETED', `Cycle tool did not complete: ${JSON.stringify(calls)}`);
  assert(findTool(calls, 'nutrition-profile')?.status === 'COMPLETED', `Nutrition tool did not complete: ${JSON.stringify(calls)}`);
  assert(findTool(calls, 'exercise-profile')?.status === 'COMPLETED', `Exercise tool did not complete: ${JSON.stringify(calls)}`);
  assert(findTool(calls, 'recent-wellness-signals')?.status === 'NO_DATA' || findTool(calls, 'recent-wellness-signals')?.status === 'COMPLETED', `Signals tool did not return a valid bounded outcome: ${JSON.stringify(calls)}`);
  assert(!JSON.stringify(body).includes('E2e-password-123!'), 'Password leaked into response');
}

async function testConsentDisabled(user) {
  await setConsent(user, false);
  const body = await chat(user, 'When is my next period and what food or exercise can help with PMS?');
  assert(typeof body.answer === 'string' && body.answer.length > 0, 'Consent-disabled general answer is empty');
  const calls = findToolCalls(body);
  assert(calls.length > 0, 'Consent-disabled intent did not produce tool status');
  assert(calls.every(tool => tool.status === 'SKIPPED'), `Consent-disabled tool was not skipped: ${JSON.stringify(calls)}`);
  assert(!JSON.stringify(body).includes('2026-09-29'), 'Cycle fact leaked while consent was disabled');
}

async function testReplay(user) {
  await setConsent(user, true);
  const requestId = crypto.randomUUID();
  const body = JSON.stringify({
    requestId,
    tenantSlug,
    externalUserId: user.userId,
    sessionId: crypto.randomUUID(),
    toolName: 'user-preferences',
    scopes: ['preferences:read'],
    authorizationJti: crypto.randomUUID(),
    aiCoachEnabled: true,
    locale: 'en-US',
    parameters: { externalUserId: user.userId, locale: 'en-US' },
  });
  const timestamp = new Date().toISOString();
  const headers = {
    'Content-Type': 'application/json',
    'X-AIF-Tenant': tenantSlug,
    'X-AIF-Timestamp': timestamp,
    'X-AIF-Signature': hmac(timestamp, body),
    'X-AIF-Request-Id': requestId,
    'X-AIF-Key-Id': callbackKeyId,
  };
  await expectStatus(`${authUrl}/aif/tools/user-preferences`, { method: 'POST', headers, body }, 200, 'first signed callback');
  await expectStatus(`${authUrl}/aif/tools/user-preferences`, { method: 'POST', headers, body }, [401, 403], 'replayed signed callback');
  await expectStatus(`${authUrl}/aif/tools/user-preferences`, { method: 'POST', headers: { ...headers, 'X-AIF-Signature': '00'.repeat(32) }, body }, [401, 403], 'tampered signed callback');

  const cycleRequestId = crypto.randomUUID();
  const cycleBody = JSON.stringify({
    requestId: cycleRequestId,
    tenantSlug,
    externalUserId: user.userId,
    sessionId: crypto.randomUUID(),
    toolName: 'cycle-summary',
    scopes: ['cycle:read'],
    authorizationJti: crypto.randomUUID(),
    aiCoachEnabled: true,
    locale: 'en-US',
    parameters: { externalUserId: user.userId, locale: 'en-US' },
  });
  const cycleTimestamp = new Date().toISOString();
  const cycleHeaders = {
    'Content-Type': 'application/json',
    'X-AIF-Tenant': tenantSlug,
    'X-AIF-Timestamp': cycleTimestamp,
    'X-AIF-Signature': hmac(cycleTimestamp, cycleBody),
    'X-AIF-Request-Id': cycleRequestId,
    'X-AIF-Key-Id': callbackKeyId,
  };
  await expectStatus(`${cyclesUrl}/api/aif/tools/cycle-summary`, { method: 'POST', headers: cycleHeaders, body: cycleBody }, 200, 'first cycles signed callback');
  await expectStatus(`${cyclesUrl}/api/aif/tools/cycle-summary`, { method: 'POST', headers: cycleHeaders, body: cycleBody }, [401, 403], 'replayed cycles signed callback');
}

async function main() {
  assert(fs.existsSync(flowelleDir), `Flowelle repository not found: ${flowelleDir}`);
  run('docker', ['info']);
  run('docker', ['compose', 'version']);
  fs.rmSync(runtimeDir, { recursive: true, force: true });
  fs.mkdirSync(runtimeDir, { recursive: true });
  const adminKeys = generateKeys();
  const userContextKey = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
  const env = [
    `AI_FRIEND_DIR=${root}`,
    `FLOWELLE_DIR=${flowelleDir}`,
    `E2E_RUNTIME_DIR=${runtimeDir}`,
    `E2E_AIF_CALLBACK_SECRET=${callbackSecret}`,
    `E2E_AIF_CALLBACK_KEY_ID=${callbackKeyId}`,
    `E2E_USER_CONTEXT_KEY_ID=${userContextKeyId}`,
    `E2E_USER_CONTEXT_PRIVATE_KEY_BASE64=${userContextKey.privateKey.export({ type: 'pkcs8', format: 'der' }).toString('base64')}`,
    `E2E_OLLAMA_MODEL=${model}`,
  ].join('\n') + '\n';
  fs.writeFileSync(path.join(runtimeDir, '.env'), env, { mode: 0o600 });

  try {
    composeStarted = true;
    compose(['up', '-d', '--build'], { capture: false });
    await waitFor(`${aiFriendUrl}/actuator/health`, 'AI-Friend');
    await waitFor(`${authUrl}/actuator/health`, 'Flowelle auth-service');
    await waitFor(`${cyclesUrl}/actuator/health`, 'Flowelle cycles-service');
    await configureAiFriend(adminKeys.admin);
    const user = await registerUser();
    await testFullConsent(user);
    await testConsentDisabled(user);
    await testReplay(user);
    console.log('REAL E2E PASS: Docker services, persistence, proxy, callbacks, consent, and replay protection verified.');
  } finally {
    if (composeStarted) {
      try {
        const logs = compose(['logs', '--no-color']);
        fs.writeFileSync(path.join(runtimeDir, 'compose.log'), logs.replace(/Bearer\s+[A-Za-z0-9._-]+/g, 'Bearer [REDACTED]'));
      } catch (error) {
        console.error(`Unable to collect Compose logs: ${error.message}`);
      }
      try {
        compose(['down', '-v', '--remove-orphans'], { capture: false });
      } catch (error) {
        console.error(`Unable to clean up Compose stack: ${error.message}`);
      }
    }
  }
}

main().catch(error => {
  console.error(`REAL E2E FAILED: ${error.stack || error.message}`);
  process.exitCode = 1;
});
