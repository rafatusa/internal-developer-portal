package com.enterprise.idp.integration;

import com.enterprise.idp.dto.auth.LoginRequest;
import com.enterprise.idp.dto.auth.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthController Integration Tests")
class AuthControllerIT extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("POST /api/v1/auth/register — creates user and returns 201 with accessToken")
    void register_returnsToken() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("ituser_" + UUID.randomUUID().toString().substring(0, 8));
        req.setEmail("ituser_" + UUID.randomUUID().toString().substring(0, 8) + "@enterprise.com");
        req.setPassword("SecurePass123");
        req.setFullName("IT Test User");

        ResponseEntity<Map> response =
            restTemplate.postForEntity(baseUrl() + "/api/v1/auth/register", req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("accessToken");
        assertThat(response.getBody().get("accessToken").toString()).isNotBlank();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — returns 200 with accessToken for admin user")
    void login_adminUser_returnsToken() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("Admin1234!");

        ResponseEntity<Map> response =
            restTemplate.postForEntity(baseUrl() + "/api/v1/auth/login", req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accessToken");
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — returns 401 for wrong password")
    void login_wrongPassword_returns401() {
        LoginRequest req = new LoginRequest();
        req.setUsername("admin");
        req.setPassword("wrongpassword");

        ResponseEntity<Map> response =
            restTemplate.postForEntity(baseUrl() + "/api/v1/auth/login", req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET /actuator/health — returns 200 and UP status")
    void healthEndpoint_returns200() {
        ResponseEntity<Map> response =
            restTemplate.getForEntity(baseUrl() + "/actuator/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }
}
