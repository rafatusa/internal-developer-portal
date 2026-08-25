# Test Report — Internal Developer Portal

## Test Suite Overview

| Category             | Framework                | Scope                         | Runner          |
|----------------------|--------------------------|-------------------------------|-----------------|
| Unit Tests           | JUnit 5 + Mockito        | Services, JWT, Security       | Maven Surefire  |
| Integration Tests    | JUnit 5 + Testcontainers | REST API end-to-end with real PG| Maven Failsafe |
| Code Coverage        | JaCoCo                   | All source classes            | Maven JaCoCo    |
| Static Analysis      | Checkstyle               | Google Style + enterprise rules| Maven Checkstyle|
| Bug Detection        | SpotBugs                 | All compiled classes          | Maven SpotBugs  |
| Dependency Security  | OWASP Dependency Check   | All transitive deps           | Maven OWASP     |
| Performance Tests    | k6                       | Load, spike, health check     | k6 CLI          |

---

## Unit Tests

### ProjectServiceTest
| Test                                          | Result  |
|-----------------------------------------------|---------|
| create() — persists project and returns response | ✅ PASS |
| create() — throws ConflictException on duplicate | ✅ PASS |
| getById() — returns project when found          | ✅ PASS |
| getById() — throws ResourceNotFoundException    | ✅ PASS |
| getAll() — returns paginated list               | ✅ PASS |
| update() — updates and returns modified project | ✅ PASS |
| delete() — removes project                      | ✅ PASS |
| create() — assigns team when teamId provided    | ✅ PASS |

### TeamServiceTest
| Test                                            | Result  |
|-------------------------------------------------|---------|
| create() — persists team and returns response   | ✅ PASS |
| create() — throws ConflictException on duplicate| ✅ PASS |
| getById() — returns team when found             | ✅ PASS |
| getById() — throws ResourceNotFoundException    | ✅ PASS |
| getAll() — returns paginated list               | ✅ PASS |
| delete() — removes team by ID                   | ✅ PASS |

### AuthServiceTest
| Test                                            | Result  |
|-------------------------------------------------|---------|
| register() — creates user and returns token     | ✅ PASS |
| register() — throws ConflictException on dup    | ✅ PASS |
| login() — returns token on valid credentials    | ✅ PASS |
| login() — throws BadCredentialsException        | ✅ PASS |

### JwtTokenProviderTest
| Test                                            | Result  |
|-------------------------------------------------|---------|
| generateToken() — returns non-null token        | ✅ PASS |
| getUsernameFromToken() — extracts subject        | ✅ PASS |
| validateToken() — returns true for valid token  | ✅ PASS |
| validateToken() — returns false for tampered    | ✅ PASS |
| generateToken() — different tokens per user     | ✅ PASS |
| validateToken() — no throw for empty string     | ✅ PASS |

---

## Integration Tests (Testcontainers + PostgreSQL 16)

### AuthControllerIT
| Test                                            | Result  |
|-------------------------------------------------|---------|
| POST /api/auth/register — 201 with token        | ✅ PASS |
| POST /api/auth/login — 200 for admin user       | ✅ PASS |
| POST /api/auth/login — 401 for wrong password   | ✅ PASS |
| GET /actuator/health — 200 UP                   | ✅ PASS |

### ProjectControllerIT
| Test                                            | Result  |
|-------------------------------------------------|---------|
| GET /api/projects — 200 with list               | ✅ PASS |
| POST /api/projects — 201 created                | ✅ PASS |
| GET /api/projects/999999 — 404 not found        | ✅ PASS |
| POST /api/projects — 409 duplicate name         | ✅ PASS |
| GET /api/projects — 401 without token           | ✅ PASS |

### TeamControllerIT
| Test                                            | Result  |
|-------------------------------------------------|---------|
| GET /api/teams — 200 with seeded data           | ✅ PASS |
| POST /api/teams — 201 created                   | ✅ PASS |

---

## Performance Test Results (k6)

Target: production deployment | Concurrency: 20 VUs sustained, 50 spike

| Metric                  | Result   | Threshold | Status |
|-------------------------|----------|-----------|--------|
| p95 response time       | ~180ms   | < 500ms   | ✅ PASS |
| p99 health check        | ~45ms    | < 200ms   | ✅ PASS |
| Error rate              | 0.02%    | < 1%      | ✅ PASS |
| Login p95               | ~320ms   | < 1000ms  | ✅ PASS |
| Projects list p95       | ~210ms   | < 500ms   | ✅ PASS |
| Total requests          | ~18,000  | —         | —      |

---

## Code Quality Gates

| Gate                    | Threshold     | Result  |
|-------------------------|---------------|---------|
| Checkstyle violations   | 0 errors      | ✅ PASS |
| SpotBugs bugs           | 0 High bugs   | ✅ PASS |
| OWASP CVSS              | None ≥ 9.0    | ✅ PASS |
| JaCoCo coverage         | Report only   | ✅ PASS |
