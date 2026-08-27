package com.example.internaldeveloperportal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the {@code app.jwt.*} configuration namespace. */
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** HMAC signing secret. Must be at least 32 bytes for HS256. */
    private String secret = "";

    /** Token lifetime in milliseconds. */
    private long expirationMs = 3600000L;

    /** Value placed in the {@code iss} claim. */
    private String issuer = "internal-developer-portal";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
