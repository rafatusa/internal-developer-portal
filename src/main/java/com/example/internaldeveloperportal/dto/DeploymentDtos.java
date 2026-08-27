package com.example.internaldeveloperportal.dto;

import com.example.internaldeveloperportal.domain.Deployment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request/response payloads for deployment resources. */
public final class DeploymentDtos {

    private DeploymentDtos() {
    }

    /** Payload for recording or replacing a deployment. */
    public record DeploymentRequest(
        @NotBlank @Size(max = 100) String version,
        @Size(max = 64) String commitSha,
        @NotNull Deployment.Status status,
        @Size(max = 120) String triggeredBy,
        Long environmentId) {
    }

    /** Deployment representation returned to clients. */
    public record DeploymentResponse(
        Long id,
        String version,
        String commitSha,
        Deployment.Status status,
        String triggeredBy,
        Long environmentId,
        String environmentName,
        Instant createdAt) {

        public static DeploymentResponse from(Deployment deployment) {
            return new DeploymentResponse(
                deployment.getId(),
                deployment.getVersion(),
                deployment.getCommitSha(),
                deployment.getStatus(),
                deployment.getTriggeredBy(),
                deployment.getEnvironment() == null ? null : deployment.getEnvironment().getId(),
                deployment.getEnvironment() == null ? null : deployment.getEnvironment().getName(),
                deployment.getCreatedAt());
        }
    }
}
