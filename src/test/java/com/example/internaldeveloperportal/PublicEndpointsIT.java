package com.example.internaldeveloperportal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the unauthenticated surface: landing page, health probe and
 * the OpenAPI documents that the deployment pipeline depends on.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PublicEndpointsIT {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("/actuator/health returns 200 and reports UP")
    void healthIsPublicAndUp() {
        ResponseEntity<String> response = rest.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    @DisplayName("the landing page is served to anonymous visitors")
    void landingPageIsPublic() {
        ResponseEntity<String> response = rest.getForEntity("/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("the OpenAPI document is public and describes the catalogue APIs")
    void openApiDocumentIsPublic() {
        ResponseEntity<String> response = rest.getForEntity("/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("/api/teams");
        assertThat(response.getBody()).contains("/api/projects");
        assertThat(response.getBody()).contains("/api/environments");
        assertThat(response.getBody()).contains("/api/deployments");
        assertThat(response.getBody()).contains("bearerAuth");
    }
}
