# Test Report

What is tested, how, and what each suite actually proves. Results are produced
by the pipeline — this document describes the suites and the quality gates, not
a snapshot of one run.

## Test strategy

| Layer | Framework | Naming | Runner | Isolation |
|---|---|---|---|---|
| Unit | JUnit 5 + Mockito + AssertJ | `*Test.java` | Surefire | Mocked collaborators, no Spring context |
| Integration | Spring Boot Test | `*IT.java` | Failsafe (`-Pfailsafe`) | Full context, random port, H2 in-memory |
| API | Spring Boot Test + `TestRestTemplate` | `*IT.java` | Failsafe | Real HTTP over the full filter chain |
| Performance | k6 | `tests/k6/performance.js` | `perf` stage | Live deployed instance |

Naming is load-bearing. Surefire excludes `**/*IT.java`; Failsafe includes only
`**/*IT.java`. A misnamed integration test runs in the unit phase and fails.

Integration tests use H2 in `PostgreSQL` compatibility mode under the `test`
profile with `ddl-auto: create-drop`, so CI needs no database container.

---

## Unit tests

### `JwtServiceTest` — 8 tests

Token issuing and verification, the security-critical core.

| Test | Proves |
|---|---|
| `generatesTokenWithClaims` | Subject, `role` claim, issuer and an expiry after issue time are all present |
| `validatesGoodToken` | A freshly issued token verifies |
| `rejectsTamperedToken` | Mutating the signature invalidates the token |
| `rejectsGarbage` | Non-JWT input returns false rather than throwing to callers |
| `rejectsForeignSignature` | A token signed with a *different* key is rejected — the actual attack |
| `rejectsExpiredToken` | Expiry is enforced, not merely recorded |
| `refusesShortSecret` | A secret under 32 bytes fails fast at construction |
| `reportsExpirySeconds` | Advertised `expiresIn` matches configuration |

`refusesShortSecret` is deliberate: HS256 with a short key is a silent
weakness. The application refuses to start rather than run insecurely.

### `TeamServiceTest` — 8 tests

| Test | Proves |
|---|---|
| `findAllMaps` / `findByIdMaps` | Entity → DTO mapping preserves every field |
| `findByIdMissing` | Unknown id raises `ResourceNotFoundException` (→ 404) |
| `createPersists` | New teams are saved and the generated id returned |
| `createRejectsDuplicate` | Duplicate name raises `ConflictException` **and never calls `save`** |
| `updateSameName` | Keeping a name during update is not a false conflict |
| `updateRejectsRenameCollision` | Renaming onto an existing name is rejected |
| `deleteRemoves` | Delete resolves the entity first, so unknown ids 404 |

### `ProjectServiceTest` — 6 tests

Covers the association logic that is easy to get wrong: filtering by team,
linking to an existing team, failing cleanly on an unknown `teamId`, rejecting
duplicates, and **detaching** a project by passing `teamId: null`.

### `DeploymentServiceTest` — 5 tests

Ordering (newest first), attaching to an environment, failing on an unknown
`environmentId`, status transitions, and 404 on unknown ids.

---

## Integration and API tests

### `AuthFlowIT` — 5 tests

Full HTTP stack, real filter chain.

| Test | Proves |
|---|---|
| `registerThenLogin` | Register → `201` + token, then log in with the same credentials |
| `duplicateRegistrationConflicts` | Second registration returns `409` |
| `badCredentialsRejected` | Unknown user returns `401`, not `404` — no user enumeration |
| `validationRejectsShortPassword` | Bean validation returns `400` before touching the database |
| `seededAdminLogsIn` | `AdminSeeder` really ran at startup |

### `CatalogApiIT` — 7 tests

| Test | Proves |
|---|---|
| `anonymousIsRejected` | Protected endpoints return `401` without a token |
| `bogusTokenRejected` | A malformed bearer token is rejected — the filter verifies, not just parses |
| `fullCatalogueChain` | Team → project → environment → deployment created and correctly linked across four endpoints |
| `teamUpdateAndDelete` | `PUT` updates, `DELETE` returns `204`, and the resource then `404`s |
| `duplicateTeamConflicts` | Uniqueness enforced through HTTP, not just in services |
| `projectsFilterByTeam` | `?teamId=` returns only that team's projects |
| `unknownIdReturnsNotFound` | Unknown ids `404` through the exception handler |

`fullCatalogueChain` is the highest-value test in the suite: it exercises the
entire ownership graph through real HTTP with a real token.

### `PublicEndpointsIT` — 3 tests

| Test | Proves |
|---|---|
| `healthIsPublicAndUp` | `/actuator/health` returns `200` and `UP` **without** auth — the exact assertion the `verify` stage makes against the deployed host |
| `landingPageIsPublic` | `/` serves the UI anonymously |
| `openApiDocumentIsPublic` | The spec is reachable and documents all four resource groups plus `bearerAuth` |

`healthIsPublicAndUp` fails locally if a security change would break the
deployment gate — the failure surfaces in stage 2 instead of stage 8.

---

## Performance test

`tests/k6/performance.js`, run against the live instance in stage 9.

**Load profile**

| Scenario | Shape |
|---|---|
| `smoke` | 5 VUs constant, 30 s |
| `ramp` | 5 → 20 VUs over 30 s, hold 20 VUs for 60 s, ramp to 0 over 20 s |

**Exercised paths:** public health probe; authenticated reads of all four
catalogue endpoints; an anonymous request that must be rejected with `401`.

**Thresholds — breaching any one fails the stage**

| Metric | Threshold |
|---|---|
| `http_req_failed` | < 1% |
| `http_req_duration` | p95 < 800 ms, p99 < 1500 ms |
| `portal_errors` | < 1% |
| `portal_health_latency` | p95 < 300 ms |

The run writes `k6-summary.json`, archived as a pipeline artifact.

Note: thresholds are for a `t3.medium` with a co-located database. A cold JVM
inflates the first requests — the 30 s smoke scenario absorbs warm-up before
the ramp measures steady state.

---

## Quality gates

Every gate below blocks the pipeline. None may be weakened to make a build
pass.

| Gate | Tool | Failure condition |
|---|---|---|
| Style | Checkstyle 10.21 | Any violation — unused imports, star imports, missing braces, tabs, lines > 140 |
| Static analysis | SpotBugs 4.8 (`Max` effort, `Medium` threshold) | Any unsuppressed bug pattern |
| Dependency CVEs | OWASP Dependency-Check 10 | Any dependency with CVSS ≥ 7 |
| Unit tests | Surefire | Any failure |
| Integration tests | Failsafe | Any failure |
| Coverage | JaCoCo | Report generated at `target/site/jacoco` |
| Health | `verify` stage | `/actuator/health` not `200` after 12 retries |
| Performance | k6 | Any threshold breach |

SpotBugs suppressions (`spotbugs-exclude.xml`) are scoped to JPA entities and
DTOs, where generated accessors legitimately expose mutable state that Spring
manages. The OWASP suppression file is intentionally **empty** — nothing is
suppressed until a specific false positive is demonstrated and justified.

---

## Coverage summary

| Component | Covered by |
|---|---|
| JWT issuing/verification | `JwtServiceTest` (8), `CatalogApiIT` (auth paths) |
| Authentication flow | `AuthFlowIT` (5) |
| Authorization | `CatalogApiIT` (anonymous, bogus token) |
| Team CRUD | `TeamServiceTest` (8), `CatalogApiIT` (3) |
| Project CRUD | `ProjectServiceTest` (6), `CatalogApiIT` (2) |
| Environment CRUD | `CatalogApiIT` (chain) |
| Deployment CRUD | `DeploymentServiceTest` (5), `CatalogApiIT` (chain) |
| Error handling | Both IT classes (400/401/404/409) |
| Public surface | `PublicEndpointsIT` (3) |
| Live system | `verify` stage, k6 |

**Known gaps** — stated rather than papered over:

- No test for concurrent writes to the same resource (last-write-wins today).
- Environment and deployment services have no dedicated unit test class; they
  are covered through `CatalogApiIT` at the API level.
- No contract test pinning the OpenAPI document against a golden file — a
  breaking API change would pass CI. `PublicEndpointsIT` only asserts the
  document exists and mentions each resource group.
- k6 exercises read paths under load; write-heavy load is untested.
