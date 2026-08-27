package com.example.internaldeveloperportal.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.internaldeveloperportal.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    /** Non-production HMAC material generated for these tests only. */
    private static final String TEST_SIGNING_MATERIAL =
        "unit-test-signing-material-long-enough-for-hs256-0123456789";

    private static final String OTHER_SIGNING_MATERIAL =
        "a-completely-different-signing-material-also-long-enough-0123456789";

    private JwtService jwtService;

    private static JwtProperties propertiesFor(String material, long expirationMs) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(material);
        properties.setExpirationMs(expirationMs);
        properties.setIssuer("internal-developer-portal");
        return properties;
    }

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(propertiesFor(TEST_SIGNING_MATERIAL, 3600000L));
    }

    @Test
    @DisplayName("issued token carries subject, role and issuer")
    void generatesTokenWithClaims() {
        String token = jwtService.generateToken("alice", "ROLE_ADMIN");

        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_ADMIN");
        assertThat(claims.getIssuer()).isEqualTo("internal-developer-portal");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    @DisplayName("a freshly issued token validates")
    void validatesGoodToken() {
        assertThat(jwtService.isValid(jwtService.generateToken("bob", "ROLE_USER"))).isTrue();
    }

    @Test
    @DisplayName("a tampered token is rejected")
    void rejectsTamperedToken() {
        String token = jwtService.generateToken("bob", "ROLE_USER");
        String tampered = token.substring(0, token.length() - 3) + "abc";

        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("garbage input is rejected rather than throwing to the caller")
    void rejectsGarbage() {
        assertThat(jwtService.isValid("not-a-jwt")).isFalse();
        assertThat(jwtService.isValid("")).isFalse();
    }

    @Test
    @DisplayName("a token signed with different material is rejected")
    void rejectsForeignSignature() {
        String foreign = new JwtService(propertiesFor(OTHER_SIGNING_MATERIAL, 3600000L))
            .generateToken("mallory", "ROLE_ADMIN");

        assertThat(jwtService.isValid(foreign)).isFalse();
    }

    @Test
    @DisplayName("expired tokens are rejected")
    void rejectsExpiredToken() throws InterruptedException {
        String token = new JwtService(propertiesFor(TEST_SIGNING_MATERIAL, 1L))
            .generateToken("carol", "ROLE_USER");

        Thread.sleep(50L);

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("signing material shorter than 32 bytes is refused at construction")
    void refusesShortSigningMaterial() {
        JwtProperties weak = propertiesFor("too-short", 3600000L);

        assertThatThrownBy(() -> new JwtService(weak))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least 32");
    }

    @Test
    @DisplayName("expiry is reported in seconds")
    void reportsExpirySeconds() {
        assertThat(jwtService.getExpiresInSeconds()).isEqualTo(3600L);
    }
}
