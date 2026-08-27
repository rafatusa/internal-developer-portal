# Deployment Summary

One-page summary of what gets built, what it costs, and what it does not do.

## What ships

| Deliverable | Location |
|---|---|
| Spring Boot 3.4.5 REST API (Java 21) | `src/main/java/` |
| JWT authentication (HS256) | `security/`, `config/SecurityConfig.java` |
| CRUD APIs — Projects, Teams, Environments, Deployments | `controller/`, `service/` |
| PostgreSQL 16 persistence | `domain/`, `repository/` |
| OpenAPI 3 / Swagger UI | `config/OpenApiConfig.java` → `/swagger-ui.html` |
| Docker image (multi-stage, non-root, health-checked) | `Dockerfile` |
| AWS infrastructure as code | `infra/*.tf` |
| Puppet server bootstrap | `puppet/` |
| Ansible application deployment | `ansible/` |
| CI/CD pipeline (9 stages) | `.udap/pipeline.yaml` → `.github/workflows/` |
| Unit, integration, API tests | `src/test/java/` |
| k6 performance test | `tests/k6/performance.js` |
| Architecture / deployment / API / test docs | `docs/`, `README.md` |

## Infrastructure

| Resource | Specification |
|---|---|
| VPC | `10.42.0.0/16` with internet gateway |
| Subnet | Public `10.42.1.0/24`, first AZ in `us-east-1` |
| EC2 | `t3.medium`, Ubuntu 22.04 LTS, 30 GiB encrypted gp3, IMDSv2 required |
| Elastic IP | Static public address, survives instance replacement |
| Security group | Inbound 80, 443, 22; all outbound |
| IAM role | Instance profile with `AmazonSSMManagedInstanceCore` only |

All resources tagged `Project=<project>`, `ManagedBy=udap`.

## Pipeline

`lint` → `test` ∥ `security` → `build_push` → `provision` → `puppet-bootstrap`
→ `configure` → `verify` → `perf`

Stages 1–3 fan out from `lint`. Stage 8 is the acceptance gate:
`/actuator/health` must return HTTP 200 or the deployment fails.

Typical end-to-end duration on a cold account: **12–18 minutes**, dominated by
the OWASP CVE database download (first run only) and EC2 boot plus Puppet
bootstrap.

## Runtime topology

```
Internet → EIP → nginx :80 → 127.0.0.1:8080 (Spring Boot)
                                    ↓
                          portal-net → PostgreSQL :5432
```

Neither the JVM nor PostgreSQL publishes a public port. Nginx is the only
listener reachable from outside the host.

## Cost

| Item | Monthly (us-east-1, on-demand) |
|---|---|
| EC2 `t3.medium` | ~$30.00 |
| EBS 30 GiB gp3 | ~$2.40 |
| Elastic IP (attached) | $0.00 |
| Data transfer (light use) | ~$1–3 |
| **Total** | **~$34–36** |

GHCR storage for the images is free for public repositories and counted against
the account's package storage for private ones.

Reducing cost: `t3.small` (~$15/month) is adequate for a low-traffic
catalogue — set `instance_type` in `infra/variables.tf`. The database and JVM
share the host, so do not go below `t3.small`.

## Verification performed

| Check | Where |
|---|---|
| Style and static analysis clean | `lint` stage |
| Unit + integration + API tests pass | `test` stage |
| No dependency with CVSS ≥ 7 | `security` stage |
| Image builds and pushes to GHCR | `build_push` stage |
| Infrastructure applies cleanly | `provision` stage |
| Host bootstrapped and hardened | `puppet-bootstrap` stage |
| Containers running, health green on the host | `configure` stage |
| **`/actuator/health` returns HTTP 200 publicly** | `verify` stage |
| OpenAPI document served and archived | `verify` stage |
| p95 < 800 ms, error rate < 1% under 20 VUs | `perf` stage |

## Security posture

**Applied:** stateless JWT, BCrypt hashing, non-root container (uid 1500),
key-only SSH with no root login, sysctl network hardening, `ufw`, unattended
security upgrades, IMDSv2 required, encrypted root volume, least-privilege IAM,
CVE gate at CVSS ≥ 7, secrets injected at runtime and never baked into images.

**Not applied at this tier** — deliberate and documented:

| Gap | Impact | Remedy |
|---|---|---|
| No TLS | Traffic is plaintext HTTP | Requires a domain; add certbot or an ALB with ACM |
| SSH open to `0.0.0.0/0` | Larger attack surface | Set `ssh_ingress_cidr` to a corporate range |
| No token revocation | A leaked token is valid up to 1 hour | Add a deny-list or refresh tokens |
| `ddl-auto: update` | Schema drift risk | Adopt Flyway before the first breaking change |

## Operational limitations

| Limitation | Consequence |
|---|---|
| Single instance, single AZ | Deploys interrupt service; instance loss is an outage |
| PostgreSQL on the app host | No automated backups, no PITR, no failover |
| No log aggregation | Diagnosis requires SSH to the host |
| No alerting | Failures are discovered by users, not by monitoring |

## Recommended next steps, in order

1. **Migrate PostgreSQL to RDS** — the highest-value change. Removes the single
   worst durability risk. Touches `infra/` and `DB_URL` only.
2. **Add TLS** — a domain plus certbot in the Ansible play, or an ALB with ACM.
3. **Restrict SSH ingress** — a one-line variable change.
4. **Adopt Flyway** — before the schema changes in a breaking way.
5. **Add monitoring and alerting** — CloudWatch agent or Prometheus scraping the
   existing actuator metrics endpoint.
6. **Add an ALB and a second instance** — the application is already stateless,
   so this needs no code change once the database is external.
