package com.enterprise.idp.integration;

import com.enterprise.idp.dto.auth.LoginRequest;
import com.enterprise.idp.dto.auth.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AuthController.
 * TestRestTemplate and baseUrl() are inherited from BaseIntegrationTest.
 */
@DisplayName("AuthController Integration Tests")
class AuthControllerIT extends BaseIntegrationTest {

    private String uniqueUsername() {
        return "authit_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — creates user and returns 201 with accessToken")
    void register_returnsToken() {
        String username = uniqueUsername();

        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setEmail(username + "@enterprise.com");
        req.setPassword("SecurePass123abc");
        req.setFullName("Auth IT User");

        ResponseEntity<Map> response =
            restTemplate.postForEntity(baseUrl() + "/api/v1/auth/register", req, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("accessToken");
        assertThat(response.getBody().get("accessToken").toString()).isNotBlank();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — returns 200 with accessToken for registered user")
    void login_registeredUser_returnsToken() {
        String username = uniqueUsername();
        String password = "LoginPass456abc";

        RegisterRequest register = new RegisterRequest();
        register.setUsername(username);
        register.setEmail(username + "@enterprise.com");
        register.setPassword(password);
        register.setFullName("Login Test User");
        restTemplate.postForEntity(baseUrl() + "/api/v1/auth/register", register, Map.class);

        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword(password);

        ResponseEntity<Map> response =
            restTemplate.postForEntity(baseUrl() + "/api/v1/auth/login", login, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accessToken");
        assertThat(response.getBody().get("accessToken").toString()).isNotBlank();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login — returns 401 for wrong password")
    void login_wrongPassword_returns401() {
        String username = uniqueUsername();

        RegisterRequest register = new RegisterRequest();
        register.setUsername(username);
        register.setEmail(username + "@enterprise.com");
        register.setPassword("CorrectPass789abc");
        register.setFullName("Wrong Pass User");
        restTemplate.postForEntity(baseUrl() + "/api/v1/auth/register", register, Map.class);

        LoginRequest req = new LoginRequest();
        req.setUsername(username);
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
