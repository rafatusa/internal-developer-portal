package com.example.internaldeveloperportal.security;

import com.example.internaldeveloperportal.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Issues and validates HS256 JSON Web Tokens for portal authentication.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;
    private final String issuer;

    public JwtService(JwtProperties properties) {
        byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "app.jwt.secret must be at least 32 characters for HS256 signing");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = properties.getExpirationMs();
        this.issuer = properties.getIssuer();
    }

    /**
     * Issues a signed token for the given subject and role.
     *
     * @param username the token subject
     * @param role     the granted authority stored in the {@code role} claim
     * @return a compact serialized JWT
     */
    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(expirationMs)))
            .signWith(signingKey)
            .compact();
    }

    /**
     * Parses and verifies a token.
     *
     * @param token compact serialized JWT
     * @return the verified claims
     * @throws JwtException if the signature, issuer or expiry is invalid
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .requireIssuer(issuer)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * @param token compact serialized JWT
     * @return true when the token is well formed, correctly signed and unexpired
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /** @return the number of seconds a freshly issued token remains valid */
    public long getExpiresInSeconds() {
        return expirationMs / 1000L;
    }
}
