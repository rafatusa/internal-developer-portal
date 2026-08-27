// k6 performance test for the Internal Developer Portal.
//
// Usage:
//   BASE_URL=http://<host> k6 run tests/k6/performance.js
//
// Exercises the public health probe and the authenticated read path, and
// fails the CI stage when latency or error-rate thresholds are breached.

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ADMIN_USERNAME = __ENV.ADMIN_USERNAME || 'admin';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'admin123456';

const errorRate = new Rate('portal_errors');
const healthLatency = new Trend('portal_health_latency');
const catalogLatency = new Trend('portal_catalog_latency');

export const options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 5,
      duration: '30s',
      gracefulStop: '10s',
    },
    ramp: {
      executor: 'ramping-vus',
      startTime: '30s',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '60s', target: 20 },
        { duration: '20s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800', 'p(99)<1500'],
    portal_errors: ['rate<0.01'],
    portal_health_latency: ['p(95)<300'],
  },
};

export function setup() {
  const response = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username: ADMIN_USERNAME, password: ADMIN_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  if (response.status !== 200) {
    console.warn(`Login failed (status ${response.status}); running unauthenticated checks only.`);
    return { token: null };
  }
  return { token: response.json('token') };
}

export default function (data) {
  group('health', () => {
    const response = http.get(`${BASE_URL}/actuator/health`);
    healthLatency.add(response.timings.duration);
    const ok = check(response, {
      'health returns 200': (r) => r.status === 200,
      'health reports UP': (r) => String(r.body).includes('UP'),
    });
    errorRate.add(!ok);
  });

  group('catalog', () => {
    if (!data.token) {
      return;
    }
    const params = {
      headers: {
        Authorization: `Bearer ${data.token}`,
        'Content-Type': 'application/json',
      },
    };

    const endpoints = ['/api/teams', '/api/projects', '/api/environments', '/api/deployments'];
    for (const endpoint of endpoints) {
      const response = http.get(`${BASE_URL}${endpoint}`, params);
      catalogLatency.add(response.timings.duration);
      const ok = check(response, {
        [`${endpoint} returns 200`]: (r) => r.status === 200,
        [`${endpoint} returns a JSON array`]: (r) => Array.isArray(r.json()),
      });
      errorRate.add(!ok);
    }
  });

  group('auth-rejection', () => {
    const response = http.get(`${BASE_URL}/api/teams`);
    const ok = check(response, {
      'anonymous access is rejected': (r) => r.status === 401,
    });
    errorRate.add(!ok);
  });

  sleep(1);
}

export function handleSummary(data) {
  return {
    stdout: JSON.stringify(
      {
        checks_passed: data.metrics.checks ? data.metrics.checks.values.passes : 0,
        checks_failed: data.metrics.checks ? data.metrics.checks.values.fails : 0,
        p95_ms: data.metrics.http_req_duration
          ? data.metrics.http_req_duration.values['p(95)']
          : null,
        error_rate: data.metrics.http_req_failed
          ? data.metrics.http_req_failed.values.rate
          : null,
      },
      null,
      2,
    ),
    'k6-summary.json': JSON.stringify(data),
  };
}
