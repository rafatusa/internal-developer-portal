package com.enterprise.idp.integration;

import com.enterprise.idp.dto.auth.LoginRequest;
import com.enterprise.idp.dto.auth.RegisterRequest;
import com.enterprise.idp.dto.team.TeamRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TeamController.
 * TestRestTemplate and baseUrl() are inherited from BaseIntegrationTest.
 */
@DisplayName("TeamController Integration Tests")
class TeamControllerIT extends BaseIntegrationTest {

    private String authToken;

    @BeforeEach
    void authenticate() {
        String uniqueSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "teamit_" + uniqueSuffix;
        String password = "TestPass123abc";

        RegisterRequest register = new RegisterRequest();
        register.setUsername(username);
        register.setEmail(username + "@enterprise.com");
        register.setPassword(password);
        register.setFullName("Team IT User");
        ResponseEntity<Map> registerResponse =
            restTemplate.postForEntity(baseUrl() + "/api/v1/auth/register", register, Map.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword(password);
        ResponseEntity<Map> loginResponse =
            restTemplate.postForEntity(baseUrl() + "/api/v1/auth/login", login, Map.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        authToken = (String) loginResponse.getBody().get("accessToken");
        assertThat(authToken).isNotBlank();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.set("Authorization", "Bearer " + authToken);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @Test
    @DisplayName("GET /api/v1/teams — returns 200 with seeded teams")
    void getAll_returns200() {
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
        teamRequest.setName("IT-Team-" + UUID.randomUUID().toString().substring(0, 8));
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
