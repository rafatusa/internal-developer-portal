# internal-developer-portal — working notes

## State
- Meta approved: aws / us-east-1 / ec2 / github / spring-boot / nginx LB
- Architecture + pipeline written (rev 2), design approved, **plan approved**
- Phase: GENERATION COMPLETE — validate PASS, rehearsal partial (see below)
- NEXT: create_repo_and_push → set secrets → deploy → wait_for_run

## Decisions
- Spring Boot 3.4.5, Java 21, Maven (scaffold via Spring Initializr).
- PostgreSQL runs as a Docker container on the same EC2 host (Tier 1, no RDS)
  — RDS is the #1 recommended Tier-2 upgrade (documented in deployment-summary).
- JWT via JJWT 0.12.x, HS256, secret from JWT_SECRET. JwtService REFUSES to
  construct if the secret is < 32 bytes (fail fast, not silent weakness).
- OpenAPI via springdoc 2.8.8 → /swagger-ui.html, /v3/api-docs.
- Bootstrap split: Puppet (java, docker, users, OS hardening) → Ansible
  (nginx, container deploy, env vars, start).
- Image published to GHCR tagged with commit SHA; Ansible pulls that exact tag.
- Nginx :80 → app 127.0.0.1:8080. App/DB publish NO public port.
- Instance: t3.medium, Ubuntu 22.04, dedicated VPC 10.42.0.0/16 (user asked
  for VPC explicitly, so not reusing the default one).

## Contracts that MUST hold
### Pom requires these files to exist
checkstyle.xml, checkstyle-suppressions.xml, spotbugs-exclude.xml,
owasp-suppressions.xml
### Test naming (load-bearing)
- `*Test.java` → surefire (unit). `*IT.java` → failsafe (`-Pfailsafe`).
- Tests use H2 in-memory under the `test` profile — no Postgres in CI.
### Pipeline → Puppet/Ansible variable wiring
- puppet-bootstrap passes `sudo DEPLOY_USER=$DEPLOY_USER bash bootstrap.sh`;
  bootstrap.sh exports FACTER_deploy_user → site.pp reads $facts['deploy_user'].
  If this breaks, the docker group is added to the WRONG user.
- configure passes -e db_password/jwt_secret/admin_username/admin_password/
  image_tag/ghcr_user/ghcr_token/repo. app.env.j2 requires ALL of them
  (no defaults — a missing one is an undefined-variable error, by design).
- ADMIN_PASSWORD secret is used by BOTH the configure stage and the perf stage
  (k6 logs in as admin). Must be set before deploy.

## Verification status
- validate_project: **PASS** (90 files).
- test_project: lint OK, SpotBugs OK, unit tests OK, integration tests OK.
  OWASP stage FAILED IN SANDBOX ONLY — `java.io.IOException: File too large`
  at ~267MB while writing the NVD H2 store, then timed out at 540s.
  This is a sandbox disk limit, NOT a project defect. Did NOT weaken the gate.
  Expect it to pass on a GitHub runner (14GB disk). Added NVD caching to the
  security stage so repeat CI runs don't re-download the DB.

## Gotchas / things already fixed
- Spring Initializr scaffold main class used TABS → Checkstyle FileTabCharacter
  failed. Reindented with spaces. Any future scaffold file needs the same.
- Added @ConfigurationPropertiesScan to the main class — JwtProperties /
  BootstrapProperties would otherwise only bind via @EnableConfigurationProperties.
- Scaffold Dockerfile was root + no HEALTHCHECK + `CMD [... "--server.port=${PORT:-8080}"]`
  (exec form does NOT expand ${PORT} — it was passed literally to Java). Rewrote:
  multi-stage, non-root uid 1500, HEALTHCHECK, exec-form ENTRYPOINT.
- nginx `validate:` on the template is useless before the symlink exists —
  replaced with an explicit `nginx -t` task AFTER enabling the vhost.
- Ansible idempotency: container tasks use docker inspect to compare the
  running container's image id vs the desired one; only replace on mismatch.
- `mvn verify -Pfailsafe` runs surefire too — keep unit tests green.
- Checkstyle runs on main sources only (includeTestSourceDirectory=false).
- Known-issue warnings about ansible synchronize / copy-with-exclude /
  rds_endpoint job outputs were reviewed and do NOT apply here (builtin
  modules only; every stage reads terraform output itself).

## Remaining work
- [x] app code, config, security, tests, k6
- [x] infra/ terraform (VPC, EC2, SG, EIP, IAM)
- [x] puppet/ + ansible/
- [x] docs (README, architecture, deployment, api, test-report, summary)
- [x] validate_project PASS
- [ ] create_repo_and_push
- [ ] set_pipeline_secret: DB_PASSWORD, JWT_SECRET, ADMIN_PASSWORD (repo must
      exist first — secrets live ON the repo)
- [ ] deploy + wait_for_run + verify /actuator/health returns 200
