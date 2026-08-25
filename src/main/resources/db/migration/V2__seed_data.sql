-- V2: Seed Data for Internal Developer Portal
-- Column names match V1 DDL exactly.

INSERT INTO teams (name, description, email_distribution, slack_channel, member_count) VALUES
    ('Platform Engineering', 'Core platform infrastructure team', 'platform@enterprise.com', '#platform-eng', 5),
    ('Application Team',     'Business application developers',   'appteam@enterprise.com',  '#app-team',     8),
    ('Security Team',        'Security and compliance',           'security@enterprise.com',  '#security',     3);

-- Admin user (password: Admin1234! — bcrypt rounds=10)
INSERT INTO app_users (username, password, email, full_name, role) VALUES
    ('admin',
     '$2a$10$7Q9k4Yr5zC1bQ5W1TQgNkOH1v6TAvZnMI5qPW4OX5JJLb2zYJhRi2',
     'admin@enterprise.com',
     'Platform Admin', 'ADMIN');

-- Sample projects
INSERT INTO projects (name, description, status, repo_url, tech_stack, team_id) VALUES
    ('IDP API',      'Internal Developer Portal REST API',     'ACTIVE',   'https://github.com/enterprise/idp-api',    'Spring Boot, PostgreSQL', 1),
    ('Auth Service', 'Centralised authentication service',     'ACTIVE',   'https://github.com/enterprise/auth-svc',   'Spring Boot, Keycloak',   1),
    ('UI Portal',    'Developer portal front-end',             'ACTIVE',   'https://github.com/enterprise/idp-ui',     'React, TypeScript',       2),
    ('Legacy App',   'Legacy monolith — scheduled for sunset', 'ARCHIVED', 'https://github.com/enterprise/legacy-app', 'Java EE, Oracle',         2);

-- Sample environments (url matches entity field; no base_url, description, is_protected defaults to false)
INSERT INTO environments (name, type, url, project_id) VALUES
    ('Development', 'DEVELOPMENT', 'http://dev.idp.internal:8080',       1),
    ('Staging',     'STAGING',     'https://staging.idp.enterprise.com', 1),
    ('Production',  'PRODUCTION',  'https://idp.enterprise.com',         1);
