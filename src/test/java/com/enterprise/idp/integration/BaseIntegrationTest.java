package com.enterprise.idp.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests.
 * Spins up a real PostgreSQL container via Testcontainers.
 * JWT secret is ≥ 64 bytes (required for HS512 — Keys.hmacShaKeyFor throws WeakKeyException below this).
 */
@Testcontainers
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
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled",      () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        // HS512 minimum key length is 64 bytes (512 bits) — shorter keys throw WeakKeyException
        // and crash ApplicationContext before any test executes.
        registry.add("app.jwt.secret", () ->
            "integration-test-jwt-secret-key-exactly-64-chars-long-for-hs512x");
        registry.add("app.jwt.expiration-ms",         () -> "86400000");
        registry.add("app.jwt.refresh-expiration-ms", () -> "604800000");
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
