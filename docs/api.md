# API Reference

Base URL: `http://<host>` · Media type: `application/json`

Interactive documentation on a running instance:
**`/swagger-ui.html`** · machine-readable spec: **`/v3/api-docs`**

---

## Authentication

The API is stateless and uses JWT bearer tokens (HS256, 1 hour lifetime).

### `POST /api/auth/register`

Creates a portal user and returns a token. Public.

```json
{ "username": "alice", "password": "at-least-8-chars" }
```

`201 Created`

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "username": "alice"
}
```

| Status | Meaning |
|---|---|
| `201` | User created |
| `400` | Validation failed (username blank, password < 8 characters) |
| `409` | Username already taken |

### `POST /api/auth/login`

Exchanges credentials for a token. Public.

```json
{ "username": "admin", "password": "…" }
```

`200 OK` — same payload as register. `401` on bad credentials.

### Using the token

```
Authorization: Bearer <token>
```

Every endpoint below requires it. Anonymous or invalid-token requests get
`401 Unauthorized`.

---

## Teams

An engineering team that owns projects.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/teams` | List all teams |
| `GET` | `/api/teams/{id}` | Fetch one team |
| `POST` | `/api/teams` | Create a team → `201` + `Location` |
| `PUT` | `/api/teams/{id}` | Replace a team |
| `DELETE` | `/api/teams/{id}` | Delete a team → `204` |

**Request**

```json
{
  "name": "platform",
  "description": "Platform engineering",
  "contactEmail": "platform@example.com"
}
```

| Field | Type | Constraints |
|---|---|---|
| `name` | string | required, ≤ 120, unique |
| `description` | string | ≤ 500 |
| `contactEmail` | string | valid email, ≤ 200 |

**Response**

```json
{
  "id": 1,
  "name": "platform",
  "description": "Platform engineering",
  "contactEmail": "platform@example.com",
  "createdAt": "2026-08-27T10:15:30Z"
}
```

---

## Projects

A catalogued software project, optionally owned by a team.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/projects` | List projects — optional `?teamId=` filter |
| `GET` | `/api/projects/{id}` | Fetch one project |
| `POST` | `/api/projects` | Create a project |
| `PUT` | `/api/projects/{id}` | Replace a project |
| `DELETE` | `/api/projects/{id}` | Delete a project |

**Request**

```json
{
  "name": "checkout-service",
  "description": "Handles order checkout",
  "repositoryUrl": "https://github.com/acme/checkout-service",
  "language": "java",
  "teamId": 1
}
```

| Field | Type | Constraints |
|---|---|---|
| `name` | string | required, ≤ 120, unique |
| `description` | string | ≤ 1000 |
| `repositoryUrl` | string | ≤ 500 |
| `language` | string | ≤ 60 |
| `teamId` | number | must reference an existing team; `null` detaches |

**Response** adds the resolved `teamName` alongside `teamId`.

---

## Environments

A deployable environment belonging to a project.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/environments` | List — optional `?projectId=` filter |
| `GET` | `/api/environments/{id}` | Fetch one environment |
| `POST` | `/api/environments` | Create an environment |
| `PUT` | `/api/environments/{id}` | Replace an environment |
| `DELETE` | `/api/environments/{id}` | Delete an environment |

**Request**

```json
{
  "name": "prod",
  "tier": "PROD",
  "region": "us-east-1",
  "endpointUrl": "https://checkout.acme.com",
  "projectId": 1
}
```

| Field | Type | Constraints |
|---|---|---|
| `name` | string | required, ≤ 120 |
| `tier` | enum | required — `DEV`, `STAGING`, `PROD` |
| `region` | string | ≤ 60 |
| `endpointUrl` | string | ≤ 500 |
| `projectId` | number | must reference an existing project |

---

## Deployments

A recorded release of a project into an environment.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/deployments` | List — optional `?environmentId=` filter (newest first) |
| `GET` | `/api/deployments/{id}` | Fetch one deployment |
| `POST` | `/api/deployments` | Record a deployment |
| `PUT` | `/api/deployments/{id}` | Replace a deployment record |
| `DELETE` | `/api/deployments/{id}` | Delete a deployment record |

**Request**

```json
{
  "version": "1.4.2",
  "commitSha": "a1b2c3d4",
  "status": "SUCCEEDED",
  "triggeredBy": "ci-bot",
  "environmentId": 1
}
```

| Field | Type | Constraints |
|---|---|---|
| `version` | string | required, ≤ 100 |
| `commitSha` | string | ≤ 64 |
| `status` | enum | required — `PENDING`, `RUNNING`, `SUCCEEDED`, `FAILED`, `ROLLED_BACK` |
| `triggeredBy` | string | ≤ 120 |
| `environmentId` | number | must reference an existing environment |

---

## Operational endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/actuator/health` | public | Liveness/readiness — `200` with `{"status":"UP"}` |
| `GET` | `/actuator/info` | public | Build information |
| `GET` | `/actuator/metrics` | authenticated | Micrometer metrics |
| `GET` | `/v3/api-docs` | public | OpenAPI 3 document |
| `GET` | `/swagger-ui.html` | public | Interactive API console |
| `GET` | `/` | public | Landing page |

---

## Errors

Every error returns the same shape:

```json
{
  "timestamp": "2026-08-27T10:15:30.123Z",
  "status": 404,
  "error": "Not Found",
  "message": "Project with id 42 was not found"
}
```

| Status | When |
|---|---|
| `400 Bad Request` | Bean-validation failure; `message` lists each offending field |
| `401 Unauthorized` | Missing, malformed, expired or wrongly-signed token; bad credentials |
| `404 Not Found` | The resource, or a referenced parent resource, does not exist |
| `409 Conflict` | Uniqueness violation (duplicate team, project or username) |

---

## Worked example

```bash
HOST=http://<host>

TOKEN=$(curl -sX POST $HOST/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<ADMIN_PASSWORD>"}' | jq -r .token)
AUTH="Authorization: Bearer $TOKEN"

TEAM=$(curl -sX POST $HOST/api/teams -H "$AUTH" -H 'Content-Type: application/json' \
  -d '{"name":"platform","contactEmail":"platform@acme.com"}' | jq -r .id)

PROJECT=$(curl -sX POST $HOST/api/projects -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"name\":\"checkout\",\"language\":\"java\",\"teamId\":$TEAM}" | jq -r .id)

ENV=$(curl -sX POST $HOST/api/environments -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"name\":\"prod\",\"tier\":\"PROD\",\"region\":\"us-east-1\",\"projectId\":$PROJECT}" | jq -r .id)

curl -sX POST $HOST/api/deployments -H "$AUTH" -H 'Content-Type: application/json' \
  -d "{\"version\":\"1.0.0\",\"status\":\"SUCCEEDED\",\"triggeredBy\":\"ci\",\"environmentId\":$ENV}"

curl -s "$HOST/api/deployments?environmentId=$ENV" -H "$AUTH" | jq
```
