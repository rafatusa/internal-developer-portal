package com.enterprise.idp.integration;

import com.enterprise.idp.domain.project.ProjectStatus;
import com.enterprise.idp.dto.auth.LoginRequest;
import com.enterprise.idp.dto.project.ProjectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProjectController Integration Tests")
class ProjectControllerIT extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String authToken;

    @BeforeEach
    void authenticate() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("Admin1234!");

        ResponseEntity<Map> loginResponse =
            restTemplate.postForEntity(baseUrl() + "/api/v1/auth/login", loginRequest, Map.class);
        if (loginResponse.getBody() != null && loginResponse.getBody().containsKey("accessToken")) {
            authToken = (String) loginResponse.getBody().get("accessToken");
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Test
    @DisplayName("GET /api/v1/projects — returns 200 with list")
    void getAll_returns200() {
        HttpEntity<Void> req = new HttpEntity<>(authHeaders());
        ResponseEntity<Map> response =
            restTemplate.exchange(baseUrl() + "/api/v1/projects", HttpMethod.GET, req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("content");
    }

    @Test
    @DisplayName("POST /api/v1/projects — creates project and returns 201")
    void createProject_returns201() {
        ProjectRequest projectRequest = new ProjectRequest();
        projectRequest.setName("Integration Test Project " + System.currentTimeMillis());
        projectRequest.setDescription("Created by integration test");
        projectRequest.setStatus(ProjectStatus.ACTIVE);
        projectRequest.setRepoUrl("https://github.com/enterprise/it-project");
        projectRequest.setTechStack("Spring Boot, PostgreSQL");

        HttpEntity<ProjectRequest> req = new HttpEntity<>(projectRequest, authHeaders());
        ResponseEntity<Map> response =
            restTemplate.exchange(baseUrl() + "/api/v1/projects", HttpMethod.POST, req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody().get("name").toString()).startsWith("Integration Test Project");
    }

    @Test
    @DisplayName("GET /api/v1/projects/{id} — returns 404 for unknown id")
    void getById_notFound_returns404() {
        HttpEntity<Void> req = new HttpEntity<>(authHeaders());
        ResponseEntity<Map> response =
            restTemplate.exchange(
                baseUrl() + "/api/v1/projects/999999", HttpMethod.GET, req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("POST /api/v1/projects — returns 409 for duplicate name")
    void createProject_duplicateName_returns409() {
        ProjectRequest req1 = new ProjectRequest();
        req1.setName("Duplicate-Project-IT");
        req1.setDescription("First");
        req1.setStatus(ProjectStatus.ACTIVE);
        req1.setRepoUrl("https://github.com/enterprise/dup1");

        HttpEntity<ProjectRequest> firstReq = new HttpEntity<>(req1, authHeaders());
        restTemplate.exchange(baseUrl() + "/api/v1/projects", HttpMethod.POST, firstReq, Map.class);

        HttpEntity<ProjectRequest> secondReq = new HttpEntity<>(req1, authHeaders());
        ResponseEntity<Map> response =
            restTemplate.exchange(
                baseUrl() + "/api/v1/projects", HttpMethod.POST, secondReq, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("GET /api/v1/projects — returns 401 without auth token")
    void getAll_noToken_returns401() {
        ResponseEntity<Map> response =
            restTemplate.getForEntity(baseUrl() + "/api/v1/projects", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
