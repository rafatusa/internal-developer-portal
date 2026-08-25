package com.enterprise.idp.dto.environment;

import com.enterprise.idp.domain.environment.EnvironmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

/**
 * Response DTO for an Environment.
 */
@Data
@Schema(description = "Environment details")
public class EnvironmentResponse {

    @Schema(description = "Environment ID")
    private Long id;

    @Schema(description = "Environment name")
    private String name;

    @Schema(description = "Environment type")
    private EnvironmentType type;

    @Schema(description = "Base URL")
    private String url;

    @Schema(description = "Cloud provider")
    private String cloudProvider;

    @Schema(description = "Cloud region")
    private String region;

    @Schema(description = "Protected flag")
    private boolean isProtected;

    @Schema(description = "Project ID")
    private Long projectId;

    @Schema(description = "Project name")
    private String projectName;

    @Schema(description = "Created at")
    private Instant createdAt;

    @Schema(description = "Updated at")
    private Instant updatedAt;
}
