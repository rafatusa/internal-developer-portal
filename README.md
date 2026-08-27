# Internal Developer Portal

A service catalogue for enterprise platform teams. Registers **Teams**, the
**Projects** they own, the **Environments** those projects deploy into, and the
**Deployments** that have shipped — behind JWT authentication, with an OpenAPI
contract and a fully automated path from commit to a running host on AWS.

| | |
|---|---|
| **Runtime** | Java 21 · Spring Boot 3.4.5 |
| **Database** | PostgreSQL 16 (container on the application host) |
| **Auth** | JWT (HS256, JJWT 0.12) |
| **Docs** | OpenAPI 3 / Swagger UI (springdoc) |
| **Packaging** | Docker multi-stage → GHCR |
| **Infrastructure** | Terraform → VPC · EC2 · Security Group · Elastic IP · IAM role |
| **Bootstrap** | Puppet (Java, Docker, users, OS hardening) |
| **Deployment** | Ansible (nginx, container, env, start) |
| **CI/CD** | GitHub Actions — 9 stages, lint through performance test |

---

## Architecture

```
Developer ──HTTP──▶ Elastic IP ──▶ nginx :80 ──proxy──▶ Spring Boot :8080
                                                              │
                                                              ▼
                                                    PostgreSQL :5432
                                                    (container, private
                                                     docker network)

GHCR ──docker pull──▶ EC2 host
```

The application binds `127.0.0.1:8080` only. Nginx is the sole publicly
reachable listener, so the API is never exposed on a high port. The database
listens only on the private container network — it publishes no host port.

The authoritative diagram is [`.udap/architecture.d2`](.udap/architecture.d2);
see [`docs/architecture.md`](docs/architecture.md) and
[`docs/deployment.md`](docs/deployment.md) for the rendered views and rationale.

---

## API

Full reference: [`docs/api.md`](docs/api.md). Live documentation is served by
the running instance:

- Swagger UI — `http://<host>/swagger-ui.html`
- OpenAPI JSON — `http://<host>/v3/api-docs`

### Authentication

```bash
# Obtain a token
curl -sX POST http://<host>/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<admin password>"}'

# {"token":"eyJhbGciOiJIUzI1NiJ9...","tokenType":"Bearer","expiresIn":3600,...}
```

### Using the catalogue

```bash
TOKEN=<token from above>

curl -sX POST http://<host>/api/teams \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"platform","description":"Platform engineering","contactEmail":"platform@example.com"}'

curl -s http://<host>/api/projects -H "Authorization: Bearer $TOKEN"
```

| Resource | Endpoints |
|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login` |
| Teams | `GET|POST /api/teams`, `GET|PUT|DELETE /api/teams/{id}` |
| Projects | `GET|POST /api/projects` (`?teamId=`), `GET|PUT|DELETE /api/projects/{id}` |
| Environments | `GET|POST /api/environments` (`?projectId=`), `GET|PUT|DELETE /api/environments/{id}` |
| Deployments | `GET|POST /api/deployments` (`?environmentId=`), `GET|PUT|DELETE /api/deployments/{id}` |
| Health | `GET /actuator/health` (public) |

Everything except `/`, `/actuator/health`, `/api/auth/**` and the OpenAPI
documents requires a bearer token; anonymous requests receive `401`.

---

## Running locally

Requirements: JDK 21 and Docker.

```bash
# 1. Start PostgreSQL
docker run -d --name idp-db -p 5432:5432 \
  -e POSTGRES_DB=idp -e POSTGRES_USER=idp -e POSTGRES_PASSWORD=localdevpassword \
  postgres:16-alpine

# 2. Run the application
export DB_URL=jdbc:postgresql://localhost:5432/idp
export DB_USERNAME=idp
export DB_PASSWORD=localdevpassword
export JWT_SECRET=local-development-secret-at-least-32-characters-long
export ADMIN_PASSWORD=localadminpassword

./mvnw spring-boot:run
```

Open http://localhost:8080 — the landing page — or
http://localhost:8080/swagger-ui.html for the API console.

### Configuration

Every setting is environment-driven (see [`.env.example`](.env.example)):

| Variable | Purpose | Required |
|---|---|---|
| `DB_URL` | JDBC URL of the PostgreSQL instance | yes in production |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials | yes in production |
| `JWT_SECRET` | HMAC signing key, **minimum 32 characters** | yes |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | Seeded administrator account | yes |

The application refuses to start when `JWT_SECRET` is shorter than 32
characters — HS256 requires it, and a short key is a silent security defect.

---

## Testing

```bash
./mvnw test                  # unit tests
./mvnw verify -Pfailsafe     # unit + integration/API tests
./mvnw checkstyle:check      # style gate
./mvnw spotbugs:check        # static analysis
./mvnw org.owasp:dependency-check-maven:check   # CVE scan (fails at CVSS >= 7)

BASE_URL=http://localhost:8080 k6 run tests/k6/performance.js  # performance
```

Integration tests run against in-memory H2 under the `test` profile, so no
database is required in CI. Naming is load-bearing: `*Test` runs under
Surefire, `*IT` under Failsafe.

See [`docs/test-report.md`](docs/test-report.md) for coverage and what each
suite proves.

---

## Deployment

Push to `main` — the pipeline does the rest.

| # | Stage | What it does |
|---|---|---|
| 1 | `lint` | Checkstyle + SpotBugs |
| 2 | `test` | Unit tests, then integration/API tests |
| 3 | `security` | OWASP dependency check |
| 4 | `build_push` | Maven package → Docker build → push to GHCR |
| 5 | `provision` | `terraform apply` — VPC, EC2, SG, EIP, IAM |
| 6 | `puppet-bootstrap` | Java, Docker, users, OS hardening |
| 7 | `configure` | Ansible — nginx, containers, env, start |
| 8 | `verify` | `/actuator/health` must return 200; archives the OpenAPI spec |
| 9 | `perf` | k6 load test against the live instance |

Stages 1–3 fan out from `lint`; the rest are sequential. The pipeline is
defined in [`.udap/pipeline.yaml`](.udap/pipeline.yaml) — the workflow files
under `.github/workflows/` are **rendered from it** and should not be edited
directly.

[`docs/deployment.md`](docs/deployment.md) covers the full runbook, including
rollback and teardown.

### Required secrets

Set by the platform: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`,
`PROJECT_NAME`, `TF_STATE_BUCKET`, `SSH_USER`, `SSH_PRIVATE_KEY`,
`SSH_PUBLIC_KEY`.

Set for this project: `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_PASSWORD`.

---

## Repository layout

```
├── src/main/java/…/          Spring Boot application
│   ├── config/               Security, OpenAPI, properties, admin seeding
│   ├── controller/           REST endpoints
│   ├── domain/               JPA entities
│   ├── dto/                  Request/response records
│   ├── exception/            Error handling
│   ├── repository/           Spring Data repositories
│   ├── security/             JWT issuing and filtering
│   └── service/              Business logic
├── src/test/java/…/          Unit (*Test) and integration (*IT) tests
├── tests/k6/                 Performance test
├── infra/                    Terraform (VPC, EC2, SG, EIP, IAM)
├── puppet/                   Masterless bootstrap manifests
├── ansible/                  Deployment playbook and templates
├── docs/                     Architecture, deployment, API, test report
├── .udap/                    Architecture + pipeline sources of truth
└── Dockerfile                Multi-stage, non-root, health-checked
```

---

## Security posture

- Stateless JWT auth; BCrypt password hashing; no sessions.
- Application container runs as an unprivileged user (uid 1500).
- Host hardening via Puppet: key-only SSH, no root login, sysctl network
  hardening, `ufw`, unattended security upgrades.
- IMDSv2 required on the instance; encrypted root volume.
- Security group exposes only 80, 443 and 22.
- OWASP dependency check fails the build at CVSS ≥ 7.

**Known limitations at this tier** — deliberate, documented, not oversights:

- Traffic is HTTP. TLS needs a domain name; add certbot or an ALB with ACM.
- SSH is open to `0.0.0.0/0` (`ssh_ingress_cidr`). Narrow it to a corporate
  range in production.
- PostgreSQL is a container on the application host with a Docker volume —
  no automated backups, no failover. Move to RDS for durability.
- Single instance, single AZ: deployment causes a brief interruption.
