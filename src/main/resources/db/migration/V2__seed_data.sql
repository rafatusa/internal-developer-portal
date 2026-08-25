-- V2: Seed Data for Internal Developer Portal
-- Description: Inserts default admin user and sample data

-- Default admin user (password: Admin1234!  →  bcrypt hash)
INSERT INTO teams (name, description, email, slack_channel) VALUES
    ('Platform Engineering', 'Core platform infrastructure team', 'platform@enterprise.com', '#platform-eng'),
    ('Application Team',    'Business application developers',   'appteam@enterprise.com',  '#app-team'),
    ('Security Team',       'Security and compliance',           'security@enterprise.com',  '#security');

-- Admin user — password hash for "Admin1234!" via bcrypt rounds=10
INSERT INTO app_users (username, email, password, full_name, role, team_id) VALUES
    ('admin', 'admin@enterprise.com',
     '$2a$10$7Q9k4Yr5zC1bQ5W1TQgNkOH1v6TAvZnMI5qPW4OX5JJLb2zYJhRi2',
     'Platform Admin', 'ADMIN', 1);

-- Sample projects
INSERT INTO projects (name, description, status, repo_url, tech_stack, team_id) VALUES
    ('IDP API',        'Internal Developer Portal REST API',      'ACTIVE',    'https://github.com/enterprise/idp-api',    'Spring Boot, PostgreSQL', 1),
    ('Auth Service',   'Centralised authentication service',      'ACTIVE',    'https://github.com/enterprise/auth-svc',   'Spring Boot, Keycloak',   1),
    ('UI Portal',      'Developer portal front-end',              'ACTIVE',    'https://github.com/enterprise/idp-ui',     'React, TypeScript',       2),
    ('Legacy App',     'Legacy monolith — scheduled for sunset',  'ARCHIVED',  'https://github.com/enterprise/legacy-app', 'Java EE, Oracle',         2);

-- Sample environments
INSERT INTO environments (name, type, base_url, description, project_id) VALUES
    ('Development', 'DEVELOPMENT', 'http://dev.idp.internal:8080',     'Dev environment',        1),
    ('Staging',     'STAGING',     'https://staging.idp.enterprise.com', 'Pre-prod environment', 1),
    ('Production',  'PRODUCTION',  'https://idp.enterprise.com',        'Production environment', 1);
