package com.enterprise.idp.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests.
 *
 * <p>Uses Spring Boot 3.1+ @ServiceConnection to wire the Testcontainers PostgreSQL
 * container into ALL Spring Boot auto-configuration (DataSource AND Flyway) before
 * context startup. This is more reliable than @DynamicPropertySource which can race
 * with Flyway's own datasource construction.
 *
 * <p>Profile "integration-test" loads application-integration-test.yml for:
 * - A 88-char JWT secret (HS512 minimum is 64 bytes)
 * - refresh-expiration-ms property
 * - Flyway enabled
 */
@Testcontainers
@ActiveProfiles("integration-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("idpdb_test")
            .withUsername("idpuser")
            .withPassword("testpassword");

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
