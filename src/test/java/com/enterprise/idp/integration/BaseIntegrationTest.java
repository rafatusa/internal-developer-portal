package com.enterprise.idp.integration;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests.
 *
 * <p>Static {@link PostgreSQLContainer} is started in a static initialiser so it is
 * running before {@code @DynamicPropertySource} fires to wire its JDBC URL into Spring
 * Boot's DataSource AND Flyway auto-configuration — no localhost:5432 race condition.
 *
 * <p>{@link TestRestTemplate} is configured to use Apache HttpClient 5
 * ({@code HttpComponentsClientHttpRequestFactory}) so that a 401 response to a POST
 * with a body returns the response body normally instead of throwing
 * {@code HttpRetryException: cannot retry due to server authentication, in streaming mode}
 * (a defect of JDK {@code HttpURLConnection} in streaming mode).
 */
@ActiveProfiles("integration-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    // ── Shared PostgreSQL container ───────────────────────────────────────────
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("idpdb_test")
            .withUsername("idpuser")
            .withPassword("testpassword");

    static {
        // Start before @DynamicPropertySource is evaluated by the Spring TestContext.
        POSTGRES.start();
    }

    @BeforeAll
    static void ensurePostgresRunning() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
    }

    // ── Property wiring ───────────────────────────────────────────────────────
    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",                POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username",           POSTGRES::getUsername);
        registry.add("spring.datasource.password",           POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name",  () -> "org.postgresql.Driver");
        registry.add("spring.flyway.url",                    POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user",                   POSTGRES::getUsername);
        registry.add("spring.flyway.password",               POSTGRES::getPassword);
    }

    // ── TestRestTemplate ──────────────────────────────────────────────────────
    @LocalServerPort
    private int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * Replace JDK {@code SimpleClientHttpRequestFactory} (default) with Apache HttpClient 5.
     * Prevents {@code HttpRetryException} when a POST/PUT receives a 401 — Apache HttpClient
     * always reads the response body regardless of status code.
     */
    @PostConstruct
    void configureHttpClient() {
        restTemplate.getRestTemplate()
            .setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
