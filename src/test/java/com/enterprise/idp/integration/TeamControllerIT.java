package com.enterprise.idp.integration;

import com.enterprise.idp.dto.auth.LoginRequest;
import com.enterprise.idp.dto.team.TeamRequest;
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

@DisplayName("TeamController Integration Tests")
class TeamControllerIT extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String authToken;

    @BeforeEach
    void authenticate() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("Admin1234!");
        ResponseEntity<Map> resp =
            restTemplate.postForEntity(baseUrl() + "/api/v1/auth/login", req, Map.class);
        if (resp.getBody() != null && resp.getBody().containsKey("accessToken")) {
            authToken = (String) resp.getBody().get("accessToken");
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + authToken);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @Test
    @DisplayName("GET /api/v1/teams — returns 200 with seeded teams")
    void getAll_returns200WithSeededData() {
        HttpEntity<Void> req = new HttpEntity<>(authHeaders());
        ResponseEntity<Map> response =
            restTemplate.exchange(baseUrl() + "/api/v1/teams", HttpMethod.GET, req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("content");
    }

    @Test
    @DisplayName("POST /api/v1/teams — creates team and returns 201")
    void createTeam_returns201() {
        TeamRequest teamRequest = new TeamRequest();
        teamRequest.setName("IT-Team-" + System.currentTimeMillis());
        teamRequest.setDescription("Integration test team");
        teamRequest.setEmailDistribution("it-team@enterprise.com");
        teamRequest.setSlackChannel("#it-team");

        HttpEntity<TeamRequest> req = new HttpEntity<>(teamRequest, authHeaders());
        ResponseEntity<Map> response =
            restTemplate.exchange(baseUrl() + "/api/v1/teams", HttpMethod.POST, req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("id");
    }
}
