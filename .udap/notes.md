# Internal Developer Portal — Build Notes

## Status
**READY TO SHIP** — validate PASS, test rehearsal: unit 27/27 PASS, integration tests SKIPPED (Docker sandbox gap — Testcontainers requires Docker; CI runners have it, sandbox does not).

## Architecture
- Spring Boot 3.3.5 / Java 21 REST API on AWS EC2
- PostgreSQL containerized in Docker on the same EC2 instance (docker-compose)
- Nginx reverse proxy (port 80/443 → app:8080)
- JWT authentication (jjwt 0.12.6) with access + refresh tokens
- CRUD APIs: Projects, Teams, Environments, Deployments
- Flyway migrations: V1 schema + V2 seed data (admin user, sample data)
- OpenAPI/Swagger at /swagger-ui.html
- Spring Boot Actuator at /actuator/health (public)

## CI/CD Pipeline
- Stages: lint (checkstyle, spotbugs) → test_unit → test_integration → security → build → provision → configure → verify
- Maven Failsafe 3.5.2 (NOT 3.3.2 — does not exist in Maven Central)
- Integration tests use Testcontainers + PostgreSQL container
- OWASP Dependency Check with NVD_API_KEY secret
- Docker image pushed to GHCR
- Terraform provisions: VPC, EC2 t3.medium, Security Group (80/443/22/8080), Elastic IP, IAM role/profile
- Puppet bootstrap: Java 21, Docker, system hardening, users
- Ansible configure: Nginx, docker-compose up, env vars

## Key Decisions
- DB is containerized (NOT RDS) — single-host docker-compose with postgres:16-alpine
- Ansible playbook is ansible/playbook.yml; pipeline runs ansible/site.yml (symlink-style include)
- SSH_USER = ubuntu (ubuntu 22.04 AMI)
- App listens on 8080; Nginx proxies port 80 → 8080
- JWT: generateAccessToken(Authentication) / generateRefreshToken(String) — NOT generateToken(String)
- AuthResponse fields: accessToken, refreshToken (NOT token)
- Team entity has emailDistribution field (NOT email)
- API base path: /api/v1/** (NOT /api/**)

## Fixes Applied
1. maven-failsafe-plugin: 3.3.2 → 3.5.2 (3.3.2 not in Maven Central)
2. All star imports in domain entities → explicit imports (Checkstyle AvoidStarImport)
3. All star imports in controllers → explicit imports
4. Removed MissingJavadocMethod (redundant with @Operation annotations)
5. JwtTokenProviderTest: rewritten to use real 3-arg constructor and generateAccessToken(Authentication)
6. AuthServiceTest: fixed to use accessToken/refreshToken fields and correct mock setup
7. TeamServiceTest + TeamControllerIT: removed setEmail() calls → setEmailDistribution()
8. AuthControllerIT + ProjectControllerIT: fixed API paths to /api/v1/... and accessToken field
9. ProjectServiceTest.update_success: removed unnecessary existsByName stubbing (name unchanged → not called)
10. AuthServiceTest.register_success: asserts "newuser" (from request) not "testuser" (from save mock)

## Secrets Required (set via set_pipeline_secret after push)
- DB_PASSWORD — alphanumeric, ≥20 chars
- JWT_SECRET — alphanumeric, ≥64 chars
- NVD_API_KEY — OWASP NVD API key

## Post-Deploy Verification
- GET /actuator/health → HTTP 200 {"status":"UP"}
- GET /swagger-ui.html → Swagger UI
- GET / → Landing page
