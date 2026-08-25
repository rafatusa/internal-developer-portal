package com.enterprise.idp.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests.
 *
 * <p>Profile "integration-test" loads application-integration-test.yml which sets:
 * - flyway.enabled=true
 * - jpa.hibernate.ddl-auto=validate
 * - A 64-char JWT secret (HS512 minimum)
 * - refresh-expiration-ms
 *
 * <p>@DynamicPropertySource then overrides ONLY the datasource connection coordinates
 * with the real Testcontainers PostgreSQL port/host (which isn't known until container starts).
 */
@Testcontainers
@ActiveProfiles("integration-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("idpdb_test")
            .withUsername("idpuser")
            .withPassword("testpassword");

    @DynamicPropertySource
    static void overrideDataSourceWithTestcontainers(DynamicPropertyRegistry registry) {
        // Only the JDBC URL/user/password need to be dynamic — the container's port is random.
        // All other config (JWT secret, Flyway, JPA) comes from application-integration-test.yml.
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
