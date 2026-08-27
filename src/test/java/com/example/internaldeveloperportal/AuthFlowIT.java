package com.example.internaldeveloperportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.internaldeveloperportal.dto.AuthDtos.LoginRequest;
import com.example.internaldeveloperportal.dto.AuthDtos.RegisterRequest;
import com.example.internaldeveloperportal.dto.AuthDtos.TokenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * End-to-end authentication behaviour over the real HTTP stack.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthFlowIT {

    @Autowired
    private TestRestTemplate rest;

    @Test
    @DisplayName("a registered user can log in and receive a bearer token")
    void registerThenLogin() {
        String username = "integration-user-" + System.nanoTime();

        ResponseEntity<TokenResponse> registered = rest.postForEntity(
            "/api/auth/register",
            new RegisterRequest(username, "sup3rSecret!"),
            TokenResponse.class);

        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registered.getBody()).isNotNull();
        assertThat(registered.getBody().token()).isNotBlank();
        assertThat(registered.getBody().tokenType()).isEqualTo("Bearer");

        ResponseEntity<TokenResponse> loggedIn = rest.postForEntity(
            "/api/auth/login",
            new LoginRequest(username, "sup3rSecret!"),
            TokenResponse.class);

        assertThat(loggedIn.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loggedIn.getBody()).isNotNull();
        assertThat(loggedIn.getBody().username()).isEqualTo(username);
    }

    @Test
    @DisplayName("registering an existing username returns 409")
    void duplicateRegistrationConflicts() {
        String username = "dup-user-" + System.nanoTime();
        rest.postForEntity("/api/auth/register",
            new RegisterRequest(username, "sup3rSecret!"), TokenResponse.class);

        ResponseEntity<String> second = rest.postForEntity("/api/auth/register",
            new RegisterRequest(username, "sup3rSecret!"), String.class);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("wrong credentials return 401")
    void badCredentialsRejected() {
        ResponseEntity<String> response = rest.postForEntity("/api/auth/login",
            new LoginRequest("nobody-here", "wrongPassword1"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("a short password fails validation with 400")
    void validationRejectsShortPassword() {
        ResponseEntity<String> response = rest.postForEntity("/api/auth/register",
            new RegisterRequest("short-pw-user", "abc"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("the seeded administrator account can log in")
    void seededAdminLogsIn() {
        ResponseEntity<TokenResponse> response = rest.postForEntity("/api/auth/login",
            new LoginRequest("admin", "admin123456"), TokenResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isNotBlank();
    }
}
