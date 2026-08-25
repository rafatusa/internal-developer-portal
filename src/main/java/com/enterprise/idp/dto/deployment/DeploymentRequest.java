package com.enterprise.idp.dto.deployment;

import com.enterprise.idp.domain.deployment.DeploymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

/**
 * Request DTO for creating or updating a Deployment.
 */
@Data
@Schema(description = "Deployment creation/update payload")
public class DeploymentRequest {

    @NotBlank(message = "Version is required")
    @Schema(description = "Application version", example = "v1.4.2")
    private String version;

    @Schema(description = "Deployment status", example = "PENDING")
    private DeploymentStatus status = DeploymentStatus.PENDING;

    @Schema(description = "Git commit SHA", example = "abc123def456")
    private String commitSha;

    @Schema(description = "Deployer username", example = "jdoe")
    private String deployedBy;

    @Schema(description = "CI pipeline URL", example = "https://github.com/org/repo/actions/runs/123")
    private String pipelineUrl;

    @Schema(description = "Release notes", example = "Fixed payment bug, improved performance")
    private String notes;

    @Schema(description = "Deployment start timestamp")
    private Instant startedAt;

    @Schema(description = "Deployment completion timestamp")
    private Instant completedAt;

    @NotNull(message = "Project ID is required")
    @Schema(description = "Project ID", example = "1")
    private Long projectId;

    @NotNull(message = "Environment ID is required")
    @Schema(description = "Environment ID", example = "1")
    private Long environmentId;
}
