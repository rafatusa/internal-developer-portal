package com.example.internaldeveloperportal.dto;

import com.example.internaldeveloperportal.domain.Environment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request/response payloads for environment resources. */
public final class EnvironmentDtos {

    private EnvironmentDtos() {
    }

    /** Payload for creating or replacing an environment. */
    public record EnvironmentRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull Environment.Tier tier,
        @Size(max = 60) String region,
        @Size(max = 500) String endpointUrl,
        Long projectId) {
    }

    /** Environment representation returned to clients. */
    public record EnvironmentResponse(
        Long id,
        String name,
        Environment.Tier tier,
        String region,
        String endpointUrl,
        Long projectId,
        String projectName,
        Instant createdAt) {

        public static EnvironmentResponse from(Environment environment) {
            return new EnvironmentResponse(
                environment.getId(),
                environment.getName(),
                environment.getTier(),
                environment.getRegion(),
                environment.getEndpointUrl(),
                environment.getProject() == null ? null : environment.getProject().getId(),
                environment.getProject() == null ? null : environment.getProject().getName(),
                environment.getCreatedAt());
        }
    }
}
