package com.enterprise.idp.dto.deployment;

import com.enterprise.idp.domain.deployment.DeploymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

/**
 * Response DTO for a Deployment.
 */
@Data
@Schema(description = "Deployment details")
public class DeploymentResponse {

    @Schema(description = "Deployment ID")
    private Long id;

    @Schema(description = "Application version")
    private String version;

    @Schema(description = "Status")
    private DeploymentStatus status;

    @Schema(description = "Git commit SHA")
    private String commitSha;

    @Schema(description = "Deployer username")
    private String deployedBy;

    @Schema(description = "CI pipeline URL")
    private String pipelineUrl;

    @Schema(description = "Release notes")
    private String notes;

    @Schema(description = "Deployment start timestamp")
    private Instant startedAt;

    @Schema(description = "Deployment completion timestamp")
    private Instant completedAt;

    @Schema(description = "Project ID")
    private Long projectId;

    @Schema(description = "Project name")
    private String projectName;

    @Schema(description = "Environment ID")
    private Long environmentId;

    @Schema(description = "Environment name")
    private String environmentName;

    @Schema(description = "Created at")
    private Instant createdAt;

    @Schema(description = "Updated at")
    private Instant updatedAt;
}
