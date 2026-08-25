-- V1: Initial Schema for Internal Developer Portal
-- Uses explicit sequences (supported by both PostgreSQL and H2).
-- Entities use GenerationType.SEQUENCE — no INSERT...RETURNING needed.

-- ─── Sequences ────────────────────────────────────────────────────────────────
CREATE SEQUENCE teams_seq        START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE app_users_seq    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE projects_seq     START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE environments_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE deployments_seq  START WITH 1 INCREMENT BY 50;

-- ─── Teams ────────────────────────────────────────────────────────────────────
CREATE TABLE teams (
    id                 BIGINT         PRIMARY KEY,
    name               VARCHAR(100)   NOT NULL UNIQUE,
    description        VARCHAR(500),
    slack_channel      VARCHAR(100),
    email_distribution VARCHAR(255),
    member_count       INTEGER        NOT NULL DEFAULT 0,
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─── Users ────────────────────────────────────────────────────────────────────
CREATE TABLE app_users (
    id           BIGINT         PRIMARY KEY,
    username     VARCHAR(50)    NOT NULL UNIQUE,
    password     VARCHAR(255)   NOT NULL,
    email        VARCHAR(100)   NOT NULL UNIQUE,
    full_name    VARCHAR(100),
    role         VARCHAR(20)    NOT NULL DEFAULT 'DEVELOPER',
    enabled      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─── Projects ─────────────────────────────────────────────────────────────────
CREATE TABLE projects (
    id          BIGINT         PRIMARY KEY,
    name        VARCHAR(100)   NOT NULL UNIQUE,
    description VARCHAR(500),
    status      VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    repo_url    VARCHAR(255),
    tech_stack  VARCHAR(200),
    team_id     BIGINT         REFERENCES teams(id) ON DELETE SET NULL,
    created_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─── Environments ─────────────────────────────────────────────────────────────
CREATE TABLE environments (
    id             BIGINT         PRIMARY KEY,
    name           VARCHAR(50)    NOT NULL,
    type           VARCHAR(20)    NOT NULL DEFAULT 'DEVELOPMENT',
    url            VARCHAR(255),
    cloud_provider VARCHAR(50),
    region         VARCHAR(50),
    is_protected   BOOLEAN        NOT NULL DEFAULT FALSE,
    project_id     BIGINT         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_env_name_project UNIQUE (project_id, name)
);

-- ─── Deployments ──────────────────────────────────────────────────────────────
CREATE TABLE deployments (
    id             BIGINT         PRIMARY KEY,
    version        VARCHAR(50)    NOT NULL,
    status         VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    commit_sha     VARCHAR(40),
    deployed_by    VARCHAR(50),
    pipeline_url   VARCHAR(255),
    notes          VARCHAR(1000),
    started_at     TIMESTAMP,
    completed_at   TIMESTAMP,
    project_id     BIGINT         NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    environment_id BIGINT         NOT NULL REFERENCES environments(id) ON DELETE CASCADE,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ─── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX idx_projects_status    ON projects(status);
CREATE INDEX idx_projects_team      ON projects(team_id);
CREATE INDEX idx_environments_proj  ON environments(project_id);
CREATE INDEX idx_deployments_proj   ON deployments(project_id);
CREATE INDEX idx_deployments_env    ON deployments(environment_id);
CREATE INDEX idx_deployments_status ON deployments(status);
