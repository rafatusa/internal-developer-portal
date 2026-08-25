# 🏗️ Internal Developer Portal

> Enterprise-grade Internal Developer Portal (IDP) — REST API, JWT auth, CRUD for Projects/Teams/Environments/Deployments, deployed to AWS EC2 via a fully automated CI/CD pipeline.

[![Build](https://github.com/enterprise/internal-developer-portal/actions/workflows/deploy.yml/badge.svg)](https://github.com/enterprise/internal-developer-portal/actions)
[![Java 21](https://img.shields.io/badge/Java-21-blue?logo=openjdk)](https://adoptium.net/)
[![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-blue?logo=postgresql)](https://www.postgresql.org/)
[![Terraform](https://img.shields.io/badge/Terraform-1.6+-purple?logo=terraform)](https://www.terraform.io/)

---

## 📐 Architecture

```
Internet
   │
   ▼
[Elastic IP]
   │
[Nginx :80]  ← reverse proxy, security headers
   │
[Spring Boot :8080]  ← JWT-secured REST API
   │
[PostgreSQL :5432]  ← relational data store (Flyway migrations)
```

**Infrastructure**: VPC → Public Subnet → EC2 (t3.medium, Ubuntu 22.04) + Elastic IP  
**Bootstrap**: Puppet (Java, Docker, users, OS hardening)  
**Configuration**: Ansible (Nginx, Docker container, .env, health verification)  
**CI/CD**: GitHub Actions (build → test → scan → docker push → terraform → puppet → ansible → verify)

---

## 🚀 Quick Start — Local Development

### Prerequisites

- Docker + Docker Compose
- Java 21 (for local builds)

```bash
# Clone the repository
git clone https://github.com/enterprise/internal-developer-portal.git
cd internal-developer-portal

# Start the full stack (Postgres + App + Nginx)
docker compose up -d

# Wait for healthy status
docker compose ps

# Test the API
curl http://localhost/actuator/health

# Login as admin
curl -s -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin1234!"}' | jq .

# Access Swagger UI
open http://localhost/swagger-ui.html
```

---

## 🔐 API Authentication

```bash
# 1. Register a new user
curl -s -X POST http://localhost/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"devuser","email":"dev@example.com","password":"SecurePass123","fullName":"Dev User"}'

# 2. Login
TOKEN=$(curl -s -X POST http://localhost/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin1234!"}' | jq -r .token)

# 3. Use token
curl -H "Authorization: Bearer $TOKEN" http://localhost/api/projects
```

---

## 🏛️ Domain APIs

| Resource     | Endpoints                                      |
|--------------|------------------------------------------------|
| Projects     | GET/POST `/api/projects`, GET/PUT/DELETE `/{id}` |
| Teams        | GET/POST `/api/teams`, GET/PUT/DELETE `/{id}`    |
| Environments | GET/POST `/api/environments`, + by project       |
| Deployments  | GET/POST `/api/deployments`, + status update     |
| Auth         | POST `/api/auth/register`, POST `/api/auth/login`|
| Health       | GET `/actuator/health`                           |

See [docs/api-documentation.md](docs/api-documentation.md) for full API reference.  
Interactive: `http://<host>/swagger-ui.html`

---

## 🧪 Testing

```bash
# Unit tests
./mvnw test

# Integration tests (requires Docker for Testcontainers)
./mvnw verify -P integration-test

# Checkstyle
./mvnw checkstyle:check

# SpotBugs
./mvnw spotbugs:check

# OWASP Dependency Check (requires NVD_API_KEY)
./mvnw dependency-check:check -Denv.NVD_API_KEY=$NVD_API_KEY

# k6 Performance test (requires running app)
k6 run --env BASE_URL=http://localhost tests/performance/k6-load-test.js
```

---

## 🏗️ Infrastructure

All IaC lives in `infra/`. Terraform provisions:

| Resource        | Description                                  |
|-----------------|----------------------------------------------|
| `aws_vpc`       | Dedicated /16 VPC with DNS enabled           |
| `aws_subnet`    | Public /24 subnet in us-east-1a              |
| `aws_internet_gateway` | Internet access                       |
| `aws_security_group`  | Ports 22/80/443 open; 8080 internal-only|
| `aws_iam_role`  | SSM, CloudWatch Logs, ECR read               |
| `aws_instance`  | t3.medium Ubuntu 22.04, IMDSv2 enforced      |
| `aws_eip`       | Static public IP                             |
| `aws_cloudwatch_log_group` | 30-day log retention              |

```bash
# Local plan (platform handles actual apply)
cd infra
terraform init -backend=false
terraform validate
terraform plan -var="ssh_public_key=$(cat ~/.ssh/id_rsa.pub)" \
               -var="db_password=localtest" \
               -var="jwt_secret=localtest32charsminimumforhs256"
```

---

## ⚙️ Configuration

Application is configured via environment variables:

| Variable              | Default       | Description                   |
|-----------------------|---------------|-------------------------------|
| `DB_HOST`             | localhost     | PostgreSQL host               |
| `DB_PORT`             | 5432          | PostgreSQL port               |
| `DB_NAME`             | idpdb         | Database name                 |
| `DB_USERNAME`         | idpuser       | Database username             |
| `DB_PASSWORD`         | —             | Database password (required)  |
| `JWT_SECRET`          | —             | JWT signing key (≥ 32 chars)  |
| `SPRING_PROFILES_ACTIVE` | production | Spring profile               |
| `PORT`                | 8080          | Application listen port       |

Copy `.env.example` to `.env` for local overrides.

---

## 🔄 CI/CD Pipeline

```
maven-build → unit-tests → integration-tests
           → checkstyle → spotbugs → owasp-check
           → docker-build-push
           → terraform-provision
           → puppet-bootstrap
           → ansible-deploy
           → health-verify
```

The pipeline is defined in `.udap/pipeline.yaml` and rendered to `.github/workflows/deploy.yml`.

**Required GitHub Secrets**: see [docs/deployment-summary.md](docs/deployment-summary.md).

---

## 📁 Project Structure

```
.
├── src/
│   ├── main/java/com/enterprise/idp/
│   │   ├── config/          # OpenAPI, Security config
│   │   ├── controller/      # REST controllers
│   │   ├── domain/          # JPA entities + repositories
│   │   ├── dto/             # Request/Response DTOs
│   │   ├── exception/       # Global exception handler
│   │   ├── security/        # JWT filter, provider, UserDetails
│   │   └── service/         # Business logic
│   ├── main/resources/
│   │   ├── db/migration/    # Flyway SQL migrations
│   │   └── application.yml  # App configuration
│   └── test/java/           # Unit + Integration tests
├── infra/                   # Terraform IaC
├── puppet/                  # Puppet manifests (bootstrap)
├── ansible/                 # Ansible playbook (configure + deploy)
├── tests/performance/       # k6 load tests
├── docs/                    # API docs, deployment summary, test report
├── nginx/                   # Nginx config (local compose)
├── Dockerfile               # Multi-stage Docker build
├── docker-compose.yml       # Local development stack
├── checkstyle.xml           # Checkstyle rules
├── spotbugs-exclude.xml     # SpotBugs exclusions
└── owasp-suppressions.xml   # OWASP suppressions
```

---

## 📄 Documentation

| Document                                    | Description                        |
|---------------------------------------------|------------------------------------|
| [docs/api-documentation.md](docs/api-documentation.md)     | Full API reference    |
| [docs/deployment-summary.md](docs/deployment-summary.md)   | Infrastructure + secrets reference |
| [docs/test-report.md](docs/test-report.md)                 | Test suite results                 |
| http://\<host\>/swagger-ui.html              | Interactive API explorer           |
| http://\<host\>/api-docs                     | OpenAPI JSON spec                  |

---

## 🔒 Security

- JWT HS256 authentication (24h expiry)
- BCrypt password hashing
- Spring Security method-level auth
- IMDSv2 enforced on EC2
- SSH key-only access (password auth disabled)
- UFW firewall (22/80/443 only)
- OS hardening via Puppet (sysctl, fail2ban)
- OWASP Dependency Check gates on CVSS ≥ 9
- SpotBugs High-threshold gate
- Non-root Docker container (idpapp user)
- EBS volume encrypted at rest

---

## 📜 License

Enterprise internal use only.
