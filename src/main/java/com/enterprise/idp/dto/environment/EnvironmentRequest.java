package com.enterprise.idp.dto.environment;

import com.enterprise.idp.domain.environment.EnvironmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO for creating or updating an Environment.
 */
@Data
@Schema(description = "Environment creation/update payload")
public class EnvironmentRequest {

    @NotBlank(message = "Environment name is required")
    @Schema(description = "Environment name", example = "production")
    private String name;

    @NotNull(message = "Environment type is required")
    @Schema(description = "Environment type", example = "PRODUCTION")
    private EnvironmentType type;

    @Schema(description = "Base URL", example = "https://api.enterprise.com")
    private String url;

    @Schema(description = "Cloud provider", example = "AWS")
    private String cloudProvider;

    @Schema(description = "Cloud region", example = "us-east-1")
    private String region;

    @Schema(description = "Whether environment is protected", example = "true")
    private boolean isProtected;

    @NotNull(message = "Project ID is required")
    @Schema(description = "Owning project ID", example = "1")
    private Long projectId;
}
