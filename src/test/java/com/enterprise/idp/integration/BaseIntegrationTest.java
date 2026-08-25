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
 * <p>Uses Spring Boot 3.1+ {@code @ServiceConnection} to wire the Testcontainers PostgreSQL
 * container into ALL Spring Boot auto-configuration (DataSource AND Flyway) before context
 * startup — more reliable than {@code @DynamicPropertySource} which can race with Flyway.
 *
 * <p>Profile {@code integration-test} loads {@code application-integration-test.yml} which sets:
 * <ul>
 *   <li>A 80-char alphanumeric JWT secret (HS512 minimum is 64 bytes)</li>
 *   <li>{@code app.jwt.refresh-expiration-ms}</li>
 *   <li>{@code app.security.bcrypt-strength=4} — fast BCrypt for tests (vs 12 in production)
 *       to avoid Tomcat 60s timeout on slow CI runners</li>
 *   <li>Flyway enabled against the Testcontainers PostgreSQL</li>
 * </ul>
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
