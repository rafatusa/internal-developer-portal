package com.enterprise.idp.dto.project;

import com.enterprise.idp.domain.project.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating or updating a Project.
 */
@Data
@Schema(description = "Project creation/update payload")
public class ProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Project name must not exceed 100 characters")
    @Schema(description = "Unique project name", example = "payment-service")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Project description", example = "Handles payment processing flows")
    private String description;

    @Schema(description = "Project status", example = "ACTIVE")
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @Schema(description = "Repository URL", example = "https://github.com/enterprise/payment-service")
    private String repoUrl;

    @Schema(description = "Technology stack", example = "Java, Spring Boot, PostgreSQL")
    private String techStack;

    @Schema(description = "Owning team ID", example = "1")
    private Long teamId;
}
