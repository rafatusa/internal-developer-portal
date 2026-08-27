package com.example.internaldeveloperportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.internaldeveloperportal.domain.Deployment;
import com.example.internaldeveloperportal.domain.Environment;
import com.example.internaldeveloperportal.dto.AuthDtos.LoginRequest;
import com.example.internaldeveloperportal.dto.AuthDtos.TokenResponse;
import com.example.internaldeveloperportal.dto.DeploymentDtos.DeploymentRequest;
import com.example.internaldeveloperportal.dto.DeploymentDtos.DeploymentResponse;
import com.example.internaldeveloperportal.dto.EnvironmentDtos.EnvironmentRequest;
import com.example.internaldeveloperportal.dto.EnvironmentDtos.EnvironmentResponse;
import com.example.internaldeveloperportal.dto.ProjectDtos.ProjectRequest;
import com.example.internaldeveloperportal.dto.ProjectDtos.ProjectResponse;
import com.example.internaldeveloperportal.dto.TeamDtos.TeamRequest;
import com.example.internaldeveloperportal.dto.TeamDtos.TeamResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * API-level tests covering the full catalogue chain
 * (team → project → environment → deployment) plus authorization behaviour.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CatalogApiIT {

    @Autowired
    private TestRestTemplate rest;

    private HttpHeaders authHeaders;

    @BeforeEach
    void authenticate() {
        ResponseEntity<TokenResponse> login = rest.postForEntity("/api/auth/login",
            new LoginRequest("admin", "admin123456"), TokenResponse.class);
        assertThat(login.getBody()).isNotNull();

        authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(login.getBody().token());
    }

    private <T> HttpEntity<T> authed(T body) {
        return new HttpEntity<>(body, authHeaders);
    }

    private String unique(String prefix) {
        return prefix + "-" + System.nanoTime();
    }

    @Test
    @DisplayName("protected endpoints reject anonymous callers with 401")
    void anonymousIsRejected() {
        ResponseEntity<String> response = rest.getForEntity("/api/teams", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("a bogus bearer token is rejected with 401")
    void bogusTokenRejected() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("not.a.real.token");

        ResponseEntity<String> response = rest.exchange(
            "/api/teams", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("full catalogue chain: team → project → environment → deployment")
    void fullCatalogueChain() {
        ResponseEntity<TeamResponse> team = rest.exchange("/api/teams", HttpMethod.POST,
            authed(new TeamRequest(unique("team"), "Owns services", "team@example.com")),
            TeamResponse.class);
        assertThat(team.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(team.getBody()).isNotNull();
        Long teamId = team.getBody().id();

        ResponseEntity<ProjectResponse> project = rest.exchange("/api/projects", HttpMethod.POST,
            authed(new ProjectRequest(unique("project"), "A service",
                "https://git.example.com/svc", "java", teamId)),
            ProjectResponse.class);
        assertThat(project.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(project.getBody()).isNotNull();
        assertThat(project.getBody().teamId()).isEqualTo(teamId);
        Long projectId = project.getBody().id();

        ResponseEntity<EnvironmentResponse> environment = rest.exchange(
            "/api/environments", HttpMethod.POST,
            authed(new EnvironmentRequest("prod", Environment.Tier.PROD, "us-east-1",
                "https://svc.example.com", projectId)),
            EnvironmentResponse.class);
        assertThat(environment.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(environment.getBody()).isNotNull();
        Long environmentId = environment.getBody().id();

        ResponseEntity<DeploymentResponse> deployment = rest.exchange(
            "/api/deployments", HttpMethod.POST,
            authed(new DeploymentRequest("1.0.0", "deadbeef",
                Deployment.Status.SUCCEEDED, "ci-bot", environmentId)),
            DeploymentResponse.class);
        assertThat(deployment.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(deployment.getBody()).isNotNull();
        assertThat(deployment.getBody().environmentName()).isEqualTo("prod");
    }

    @Test
    @DisplayName("teams support update and delete")
    void teamUpdateAndDelete() {
        ResponseEntity<TeamResponse> created = rest.exchange("/api/teams", HttpMethod.POST,
            authed(new TeamRequest(unique("team"), "before", null)), TeamResponse.class);
        assertThat(created.getBody()).isNotNull();
        Long id = created.getBody().id();
        String name = created.getBody().name();

        ResponseEntity<TeamResponse> updated = rest.exchange("/api/teams/" + id, HttpMethod.PUT,
            authed(new TeamRequest(name, "after", null)), TeamResponse.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).isNotNull();
        assertThat(updated.getBody().description()).isEqualTo("after");

        ResponseEntity<Void> deleted = rest.exchange("/api/teams/" + id, HttpMethod.DELETE,
            new HttpEntity<>(authHeaders), Void.class);
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> gone = rest.exchange("/api/teams/" + id, HttpMethod.GET,
            new HttpEntity<>(authHeaders), String.class);
        assertThat(gone.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("duplicate team names are rejected with 409")
    void duplicateTeamConflicts() {
        String name = unique("team");
        rest.exchange("/api/teams", HttpMethod.POST,
            authed(new TeamRequest(name, null, null)), TeamResponse.class);

        ResponseEntity<String> second = rest.exchange("/api/teams", HttpMethod.POST,
            authed(new TeamRequest(name, null, null)), String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("projects can be filtered by team")
    void projectsFilterByTeam() {
        ResponseEntity<TeamResponse> team = rest.exchange("/api/teams", HttpMethod.POST,
            authed(new TeamRequest(unique("team"), null, null)), TeamResponse.class);
        assertThat(team.getBody()).isNotNull();
        Long teamId = team.getBody().id();

        rest.exchange("/api/projects", HttpMethod.POST,
            authed(new ProjectRequest(unique("project"), null, null, "go", teamId)),
            ProjectResponse.class);

        ResponseEntity<ProjectResponse[]> filtered = rest.exchange(
            "/api/projects?teamId=" + teamId, HttpMethod.GET,
            new HttpEntity<>(authHeaders), ProjectResponse[].class);

        assertThat(filtered.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filtered.getBody()).isNotNull().hasSize(1);
    }

    @Test
    @DisplayName("an unknown id returns 404")
    void unknownIdReturnsNotFound() {
        ResponseEntity<String> response = rest.exchange("/api/projects/999999",
            HttpMethod.GET, new HttpEntity<>(authHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
