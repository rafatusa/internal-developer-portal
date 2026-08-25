package com.enterprise.idp.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private static final String SECRET =
        "test-secret-key-must-be-at-least-32-chars-long-for-hs256-algorithm-ok";
    private static final long EXPIRY_MS = 86_400_000L;
    private static final long REFRESH_MS = 604_800_000L;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, EXPIRY_MS, REFRESH_MS);
    }

    private Authentication authFor(String username) {
        return new UsernamePasswordAuthenticationToken(
            new org.springframework.security.core.userdetails.User(
                username, "password", List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER"))),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_DEVELOPER")));
    }

    @Test
    @DisplayName("generateAccessToken() — returns non-null token for authenticated user")
    void generateAccessToken_returnsToken() {
        String token = jwtTokenProvider.generateAccessToken(authFor("testuser"));
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("generateRefreshToken() — returns non-null refresh token for username")
    void generateRefreshToken_returnsToken() {
        String token = jwtTokenProvider.generateRefreshToken("testuser");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("getUsernameFromToken() — extracts correct subject from access token")
    void getUsernameFromToken_extractsSubject() {
        String token = jwtTokenProvider.generateAccessToken(authFor("alice"));
        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertThat(username).isEqualTo("alice");
    }

    @Test
    @DisplayName("validateToken() — returns true for a valid access token")
    void validateToken_valid() {
        String token = jwtTokenProvider.generateAccessToken(authFor("bob"));
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("validateToken() — returns false for a tampered token")
    void validateToken_tampered_returnsFalse() {
        String token = jwtTokenProvider.generateAccessToken(authFor("charlie")) + "tampered";
        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("generateAccessToken() — produces different tokens for different users")
    void generateAccessToken_differentUsers_differentTokens() {
        String t1 = jwtTokenProvider.generateAccessToken(authFor("user1"));
        String t2 = jwtTokenProvider.generateAccessToken(authFor("user2"));
        assertThat(t1).isNotEqualTo(t2);
    }

    @Test
    @DisplayName("validateToken() — does not throw for empty string input")
    void validateToken_emptyString_returnsFalse() {
        assertThatCode(() -> assertThat(jwtTokenProvider.validateToken("")).isFalse())
            .doesNotThrowAnyException();
    }
}
