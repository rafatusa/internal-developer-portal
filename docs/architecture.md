# Architecture

## Context

The Internal Developer Portal is a service catalogue. Platform teams register
the teams they run, the projects those teams own, the environments projects
deploy into, and the deployments that have shipped. It answers "who owns this
service, where does it run, and what version is live?" from one API.

## Component view

```
                          ┌─────────────────────────────────────────┐
   Developer              │            AWS VPC 10.42.0.0/16         │
      │                   │                                         │
      │  HTTP :80         │   ┌──────────────────────────────────┐  │
      └──────────────────▶│   │  Public subnet 10.42.1.0/24      │  │
                          │   │                                  │  │
                Elastic IP├──▶│   EC2 t3.medium (Ubuntu 22.04)   │  │
                          │   │   ┌───────────────────────────┐  │  │
                          │   │   │ nginx :80                 │  │  │
                          │   │   │   │ proxy_pass            │  │  │
                          │   │   │   ▼                       │  │  │
                          │   │   │ Spring Boot 127.0.0.1:8080│  │  │
                          │   │   │   │ JDBC                  │  │  │
                          │   │   │   ▼                       │  │  │
                          │   │   │ PostgreSQL 16 :5432       │  │  │
                          │   │   │  (portal-net, no host     │  │  │
                          │   │   │   port published)         │  │  │
                          │   │   └───────────────────────────┘  │  │
                          │   │        ▲                         │  │
                          │   └────────┼─────────────────────────┘  │
                          │  Security Group: 80, 443, 22 in         │
                          │  IAM role: AmazonSSMManagedInstanceCore │
                          └────────────┼────────────────────────────┘
                                       │ docker pull
                                       │
                          GitHub Container Registry
```

The machine-readable source of truth is [`../.udap/architecture.d2`](../.udap/architecture.d2),
which `validate_project` cross-checks against the Terraform in `infra/`.

## Layers

### Application

Standard Spring Boot layering — controllers are thin, services hold the rules,
repositories are Spring Data interfaces:

| Package | Responsibility |
|---|---|
| `controller` | HTTP surface, validation entry, OpenAPI annotations |
| `service` | Business rules, transaction boundaries, uniqueness checks |
| `repository` | Spring Data JPA persistence |
| `domain` | JPA entities: `Team`, `Project`, `Environment`, `Deployment`, `PortalUser` |
| `dto` | Java records for requests/responses — entities never cross the HTTP boundary |
| `security` | `JwtService` (issue/verify), `JwtAuthenticationFilter` (per-request) |
| `config` | Security chain, OpenAPI document, typed properties, admin seeding |
| `exception` | `GlobalExceptionHandler` → 400 / 401 / 404 / 409 |

### Data model

```
Team 1───* Project 1───* Environment 1───* Deployment

PortalUser  (authentication only, unrelated to the catalogue graph)
```

Relations are `@ManyToOne` with `FetchType.LAZY` and `open-in-view: false`, so
serialization can never trigger a lazy load outside a transaction. DTO mappers
read association fields explicitly inside the service's transaction.

Schema management is `ddl-auto: update`. That is honest for this tier and a
known limitation: adopt Flyway before the first breaking schema change.

### Request path

1. nginx accepts on `:80` and proxies to `127.0.0.1:8080`, setting
   `Host`, `X-Real-IP`, `X-Forwarded-For` and `X-Forwarded-Proto`.
2. Spring reads those headers (`forward-headers-strategy: framework`) so
   generated URLs are correct behind the proxy.
3. `JwtAuthenticationFilter` extracts a bearer token, verifies signature,
   issuer and expiry, and populates the security context.
4. The security chain permits the landing page, health probe, auth endpoints
   and OpenAPI documents; everything else requires authentication.
5. Controller → service → repository → PostgreSQL.

## Decisions

### PostgreSQL as a container on the application host

**Chosen** over RDS. At this tier the portal is a catalogue with modest write
volume; a container plus a Docker volume costs nothing beyond the instance,
whereas RDS adds roughly $15–30/month and a subnet group.

*Cost of this choice:* no automated backups, no point-in-time recovery, no
failover, and data lives on one EBS volume. **Migrating to RDS is the first
recommended upgrade** — it changes `infra/` and the `DB_URL` variable, nothing
in the application.

### Nginx in front of the application

The JVM binds `127.0.0.1:8080` and is unreachable from outside the host. Nginx
owns port 80. This keeps the public surface to one listener, gives a place to
terminate TLS later without touching the application, and lets the health probe
have its own short-timeout location block.

### Puppet then Ansible

Two tools, two clearly separated jobs:

- **Puppet** owns the *machine*: Java, Docker, system users, OS hardening.
  Masterless `puppet apply` — no Puppet Server to operate.
- **Ansible** owns the *application*: nginx config, environment file,
  containers, start and health verification.

The boundary is "does this change when the application version changes?" If
yes, it is Ansible's. Both are idempotent, so any pipeline retry is free.

### Stateless JWT

No server-side sessions, so the instance holds no authentication state and a
future second instance needs no sticky sessions or shared session store. The
trade-off is that tokens cannot be revoked before expiry; the one-hour lifetime
bounds that exposure.

## Non-functional posture

| Concern | Position |
|---|---|
| **Availability** | Single instance, single AZ. Deployment interrupts service briefly. Docker `restart: unless-stopped` recovers from crashes; EIP keeps the address stable across instance replacement. |
| **Scalability** | Vertical only. Stateless application, so horizontal scaling needs an ALB, a shared database (RDS) and nothing else. |
| **Security** | Key-only SSH, no root login, IMDSv2 required, encrypted root volume, `ufw`, unattended security upgrades, non-root container, CVE gate at CVSS ≥ 7. |
| **Observability** | Actuator health/info/metrics; nginx access and error logs; Docker json-file logging capped at 20 MB × 5. No aggregation or alerting at this tier. |
| **Cost** | One `t3.medium` + EIP + EBS ≈ $35–40/month in `us-east-1`. |

## Deliberately omitted

Not oversights — Tier-2/3 items excluded to keep the first deployment
comprehensible:

- TLS/HTTPS (needs a domain; add certbot or an ALB with ACM)
- RDS with automated backups and multi-AZ
- Application Load Balancer and autoscaling
- Centralised logging and metrics (CloudWatch, Prometheus/Grafana)
- Flyway schema migrations
- Token revocation / refresh tokens
- Private subnets with a NAT gateway
