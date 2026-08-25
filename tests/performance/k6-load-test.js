/**
 * k6 Performance Test — Internal Developer Portal
 *
 * Usage:
 *   k6 run \
 *     --env BASE_URL=http://<host> \
 *     --env ADMIN_USERNAME=admin \
 *     --env ADMIN_PASSWORD=<password> \
 *     tests/performance/k6-load-test.js
 *
 * All credentials are injected via --env at runtime — never hardcoded.
 *
 * Stages:
 *   1. Ramp-up    : 0 → 20 VUs over 1 minute
 *   2. Sustained  : 20 VUs for 3 minutes
 *   3. Spike      : 20 → 50 VUs over 30 seconds
 *   4. Ramp-down  : 50 → 0 VUs over 30 seconds
 *
 * Thresholds:
 *   - p95 response time < 500ms
 *   - error rate < 1%
 *   - health check always < 200ms (p99)
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ─── Custom Metrics ──────────────────────────────────────────────────────────
const errorRate = new Rate('errors');
const loginTrend = new Trend('login_duration', true);
const projectsTrend = new Trend('projects_duration', true);
const healthTrend = new Trend('health_duration', true);

// ─── Runtime Configuration (all from --env flags) ────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:80';
const ADMIN_USERNAME = __ENV.ADMIN_USERNAME || 'admin';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD;

if (!ADMIN_PASSWORD) {
  throw new Error('ADMIN_PASSWORD env var is required: k6 run --env ADMIN_PASSWORD=<value> ...');
}

export const options = {
  stages: [
    { duration: '1m',  target: 20 },
    { duration: '3m',  target: 20 },
    { duration: '30s', target: 50 },
    { duration: '30s', target: 0  },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed:   ['rate<0.01'],
    errors:            ['rate<0.01'],
    login_duration:    ['p(95)<1000'],
    projects_duration: ['p(95)<500'],
    health_duration:   ['p(99)<200'],
  },
};

// ─── Setup — authenticate once per run ───────────────────────────────────────
export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username: ADMIN_USERNAME, password: ADMIN_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(loginRes, {
    'setup: login status 200': (r) => r.status === 200,
    'setup: token present':    (r) => !!r.json('token'),
  });

  return { token: loginRes.json('token') };
}

// ─── Main Virtual User Scenario ───────────────────────────────────────────────
export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    Authorization: `Bearer ${data.token}`,
  };

  group('Health Check', () => {
    const res = http.get(`${BASE_URL}/actuator/health`);
    healthTrend.add(res.timings.duration);
    const ok = check(res, {
      'health: status 200':       (r) => r.status === 200,
      'health: status is UP':     (r) => r.json('status') === 'UP',
      'health: response <200ms':  (r) => r.timings.duration < 200,
    });
    errorRate.add(!ok);
  });

  sleep(0.5);

  group('Auth — Login', () => {
    const res = http.post(
      `${BASE_URL}/api/auth/login`,
      JSON.stringify({ username: ADMIN_USERNAME, password: ADMIN_PASSWORD }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    loginTrend.add(res.timings.duration);
    const ok = check(res, {
      'login: status 200': (r) => r.status === 200,
      'login: has token':  (r) => !!r.json('token'),
    });
    errorRate.add(!ok);
  });

  sleep(0.5);

  group('Projects API', () => {
    const listRes = http.get(`${BASE_URL}/api/projects?page=0&size=10`, { headers });
    projectsTrend.add(listRes.timings.duration);
    const listOk = check(listRes, {
      'projects list: status 200':   (r) => r.status === 200,
      'projects list: has content':  (r) => r.json('content') !== null,
      'projects list: <500ms':       (r) => r.timings.duration < 500,
    });
    errorRate.add(!listOk);

    sleep(0.3);

    const projectName = `k6-perf-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
    const createRes = http.post(
      `${BASE_URL}/api/projects`,
      JSON.stringify({
        name: projectName,
        description: 'k6 performance test project',
        status: 'ACTIVE',
        repoUrl: `https://github.com/enterprise/${projectName}`,
        techStack: 'Spring Boot',
      }),
      { headers }
    );
    const createOk = check(createRes, {
      'project create: status 201': (r) => r.status === 201,
      'project create: has id':     (r) => r.json('id') !== null,
    });
    errorRate.add(!createOk);
  });

  sleep(0.5);

  group('Teams API', () => {
    const res = http.get(`${BASE_URL}/api/teams?page=0&size=10`, { headers });
    const ok = check(res, {
      'teams list: status 200': (r) => r.status === 200,
    });
    errorRate.add(!ok);
  });

  sleep(1);
}

export function teardown() {
  console.log('k6 performance test completed.');
}
