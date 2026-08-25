# Internal Developer Portal — API Documentation

## Overview

Base URL: `https://<HOST>/`
Auth: Bearer JWT (obtain from `/api/auth/login`)
Format: `application/json`
API Docs (interactive): `http://<HOST>/swagger-ui.html`
OpenAPI Spec: `http://<HOST>/api-docs`

---

## Authentication

### POST /api/auth/register

Register a new user.

**Request body**
```json
{
  "username": "devuser",
  "email": "dev@enterprise.com",
  "password": "SecurePass123",
  "fullName": "Dev User"
}
```

**Response 201**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "devuser",
  "email": "dev@enterprise.com",
  "role": "DEVELOPER",
  "expiresIn": 86400000
}
```

---

### POST /api/auth/login

Authenticate and receive a JWT.

**Request body**
```json
{ "username": "admin", "password": "Admin1234!" }
```

**Response 200** — same shape as register.
**Response 401** — invalid credentials.

---

## Projects

All project endpoints require `Authorization: Bearer <token>`.

| Method | Path                      | Description               | Auth Required |
|--------|---------------------------|---------------------------|---------------|
| GET    | /api/projects             | List projects (paginated) | ✅             |
| POST   | /api/projects             | Create project            | ✅ ADMIN/PM   |
| GET    | /api/projects/{id}        | Get project by ID         | ✅             |
| PUT    | /api/projects/{id}        | Update project            | ✅ ADMIN/PM   |
| DELETE | /api/projects/{id}        | Delete project            | ✅ ADMIN      |
| GET    | /api/projects/search      | Search by name/desc       | ✅             |
| GET    | /api/projects/by-status   | Filter by status          | ✅             |

**Query parameters (list/search)**
- `page` (default 0), `size` (default 20), `sort` (e.g. `name,asc`)
- `q` — search query (search endpoint)
- `status` — `ACTIVE | INACTIVE | ARCHIVED` (by-status endpoint)

**Project object**
```json
{
  "id": 1,
  "name": "IDP API",
  "description": "Internal Developer Portal REST API",
  "status": "ACTIVE",
  "repoUrl": "https://github.com/enterprise/idp-api",
  "techStack": "Spring Boot, PostgreSQL",
  "teamId": 1,
  "teamName": "Platform Engineering",
  "createdAt": "2024-01-15T10:00:00",
  "updatedAt": "2024-01-15T10:00:00"
}
```

---

## Teams

| Method | Path             | Description              |
|--------|------------------|--------------------------|
| GET    | /api/teams       | List teams (paginated)   |
| POST   | /api/teams       | Create team              |
| GET    | /api/teams/{id}  | Get team by ID           |
| PUT    | /api/teams/{id}  | Update team              |
| DELETE | /api/teams/{id}  | Delete team              |

**Team object**
```json
{
  "id": 1,
  "name": "Platform Engineering",
  "description": "Core platform infrastructure team",
  "email": "platform@enterprise.com",
  "slackChannel": "#platform-eng",
  "createdAt": "2024-01-15T10:00:00",
  "updatedAt": "2024-01-15T10:00:00"
}
```

---

## Environments

| Method | Path                              | Description                  |
|--------|-----------------------------------|------------------------------|
| GET    | /api/environments                 | List all environments        |
| POST   | /api/environments                 | Create environment           |
| GET    | /api/environments/{id}            | Get environment by ID        |
| PUT    | /api/environments/{id}            | Update environment           |
| DELETE | /api/environments/{id}            | Delete environment           |
| GET    | /api/environments/project/{pid}   | List by project              |

**Environment types**: `DEVELOPMENT | STAGING | PRODUCTION | DR`

---

## Deployments

| Method | Path                              | Description                 |
|--------|-----------------------------------|-----------------------------|
| GET    | /api/deployments                  | List deployments (paginated)|
| POST   | /api/deployments                  | Trigger a deployment        |
| GET    | /api/deployments/{id}             | Get deployment by ID        |
| PUT    | /api/deployments/{id}/status      | Update status               |
| GET    | /api/deployments/project/{pid}    | By project                  |
| GET    | /api/deployments/environment/{eid}| By environment              |

**Deployment statuses**: `PENDING | RUNNING | SUCCESS | FAILED | ROLLED_BACK | CANCELLED`

---

## Health & Observability

| Endpoint                   | Auth | Description                 |
|----------------------------|------|-----------------------------|
| GET /actuator/health       | No   | Application health status   |
| GET /actuator/info         | No   | Application info            |
| GET /actuator/metrics      | Yes  | Micrometer metrics          |

---

## Error Responses

All errors follow RFC 7807 Problem Details:

```json
{
  "timestamp": "2024-01-15T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Project with id 99 not found",
  "path": "/api/projects/99"
}
```

| HTTP Status | Meaning                          |
|-------------|----------------------------------|
| 400         | Validation error                 |
| 401         | Missing/invalid JWT              |
| 403         | Insufficient permissions         |
| 404         | Resource not found               |
| 409         | Conflict (duplicate name, etc.)  |
| 500         | Internal server error            |
