package com.enterprise.idp.integration;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests.
 *
 * Uses H2 in PostgreSQL-compatibility mode (configured in
 * application-integration-test.yml) — no Testcontainers, no @DynamicPropertySource,
 * no abstract-class lifecycle issues, no Docker dependency in CI.
 *
 * H2 runs in-process: Flyway migrates the schema, Hibernate validates against it,
 * the full Spring Security + controller + service + repository stack is exercised.
 *
 * TestRestTemplate uses Apache HttpClient 5 (HttpComponentsClientHttpRequestFactory)
 * to prevent HttpRetryException on 401 responses to POST requests with a body.
 */
@ActiveProfiles("integration-test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @PostConstruct
    void configureHttpClient() {
        restTemplate.getRestTemplate()
            .setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
