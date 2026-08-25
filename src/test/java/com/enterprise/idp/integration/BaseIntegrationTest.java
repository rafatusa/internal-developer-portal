package com.enterprise.idp.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for all integration tests.
 *
 * <p>Uses a manually-started static {@link PostgreSQLContainer} shared across all
 * subclasses, with {@code @DynamicPropertySource} to wire datasource coordinates into
 * Spring Boot's auto-configuration (DataSource AND Flyway) before the context starts.
 *
 * <p>Why not {@code @ServiceConnection} + {@code @Testcontainers}?
 * The JUnit 5 Testcontainers extension only lifecycle-manages {@code @Container} fields
 * declared in <em>concrete</em> test classes — it does not start containers declared
 * {@code static} on an {@code abstract} base class. The container silently stays
 * un-started, {@code @ServiceConnection} wires a null URL, every DB call returns 500.
 *
 * <p>Manual {@code start()} in a static initialiser + {@code @DynamicPropertySource}
 * is the correct shared-container pattern and works in all Spring Boot 3.x versions.
 */
@ActiveProfiles("integration-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    // One container shared across ALL subclasses — started once, reused for every context.
    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("idpdb_test")
            .withUsername("idpuser")
            .withPassword("testpassword");
        POSTGRES.start();
    }

    /**
     * Wire the running container's coordinates into Spring Boot's DataSource AND Flyway
     * auto-configuration before the ApplicationContext is created.
     *
     * <p>This fires before context startup and overrides every datasource property
     * — no race condition with Flyway, no partial binding from application.yml.
     */
    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name",
                     () -> "org.postgresql.Driver");
        // Flyway reads its own datasource independently — override it too.
        registry.add("spring.flyway.url",      POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user",     POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
