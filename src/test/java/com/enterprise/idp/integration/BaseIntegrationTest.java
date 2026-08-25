package com.enterprise.idp.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests.
 *
 * <p>Uses Spring Boot 3.1+ @ServiceConnection to wire the Testcontainers PostgreSQL
 * container into ALL Spring Boot auto-configuration (DataSource AND Flyway).
 *
 * <p>Overrides BCryptPasswordEncoder cost from 12 (production) to 4 (test) via
 * @TestConfiguration — BCrypt cost 12 takes ~500ms per hash; with register+login
 * each doing 2-3 encode/matches calls, the 60s Tomcat timeout is hit on slow runners.
 * Cost 4 is still cryptographically valid and takes ~5ms per hash.
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

    /**
     * Override BCrypt cost to 4 for integration tests.
     * Production uses cost 12 (~500ms/hash); cost 4 takes ~5ms — avoids 60s Tomcat timeout
     * when register+login each do multiple encode/matches operations per @BeforeEach.
     */
    @TestConfiguration
    static class FastPasswordEncoderConfig {

        @Bean
        @Primary
        public PasswordEncoder testPasswordEncoder() {
            return new BCryptPasswordEncoder(4);
        }
    }
}
