-- V1: Initial Schema for Internal Developer Portal
-- Author: IDP Platform
-- Description: Creates all core tables

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ─── Teams ────────────────────────────────────────────────────────────────────
CREATE TABLE teams (
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(100) NOT NULL UNIQUE,
    description        TEXT,
    email_distribution VARCHAR(255),
    slack_channel      VARCHAR(100),
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ─── Users ────────────────────────────────────────────────────────────────────
CREATE TABLE app_users (
    id           BIGSERIAL PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    email        VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    full_name    VARCHAR(150),
    role         VARCHAR(20)  NOT NULL DEFAULT 'DEVELOPER',
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    team_id      BIGINT REFERENCES teams(id) ON DELETE SET NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ─── Projects ─────────────────────────────────────────────────────────────────
CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    repo_url    VARCHAR(500),
    tech_stack  VARCHAR(255),
    team_id     BIGINT REFERENCES teams(id) ON DELETE SET NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- ─── Environments ─────────────────────────────────────────────────────────────
CREATE TABLE environments (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(20)  NOT NULL DEFAULT 'DEVELOPMENT',
    base_url    VARCHAR(500),
    description TEXT,
    project_id  BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_env_name_project UNIQUE (name, project_id)
);

-- ─── Deployments ──────────────────────────────────────────────────────────────
CREATE TABLE deployments (
    id             BIGSERIAL PRIMARY KEY,
    version        VARCHAR(100) NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    artifact_url   VARCHAR(500),
    notes          TEXT,
    deployed_at    TIMESTAMP,
    project_id     BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    environment_id BIGINT NOT NULL REFERENCES environments(id) ON DELETE CASCADE,
    deployed_by_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ─── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX idx_projects_status    ON projects(status);
CREATE INDEX idx_projects_team      ON projects(team_id);
CREATE INDEX idx_environments_proj  ON environments(project_id);
CREATE INDEX idx_deployments_proj   ON deployments(project_id);
CREATE INDEX idx_deployments_env    ON deployments(environment_id);
CREATE INDEX idx_deployments_status ON deployments(status);
CREATE INDEX idx_users_team         ON app_users(team_id);
