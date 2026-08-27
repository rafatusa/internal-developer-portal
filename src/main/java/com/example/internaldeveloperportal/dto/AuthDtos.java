package com.example.internaldeveloperportal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response payloads for the authentication endpoints. */
public final class AuthDtos {

    private AuthDtos() {
    }

    /** Credentials submitted to obtain a token. */
    public record LoginRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(min = 8, max = 200) String password) {
    }

    /** Credentials submitted to create a new portal user. */
    public record RegisterRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(min = 8, max = 200) String password) {
    }

    /** An issued bearer token. */
    public record TokenResponse(String token, String tokenType, long expiresIn, String username) {
    }
}
