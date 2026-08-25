package com.enterprise.idp.dto.project;

import com.enterprise.idp.domain.project.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

/**
 * Response DTO for a Project.
 */
@Data
@Schema(description = "Project details")
public class ProjectResponse {

    @Schema(description = "Project ID")
    private Long id;

    @Schema(description = "Project name")
    private String name;

    @Schema(description = "Project description")
    private String description;

    @Schema(description = "Status")
    private ProjectStatus status;

    @Schema(description = "Repository URL")
    private String repoUrl;

    @Schema(description = "Technology stack")
    private String techStack;

    @Schema(description = "Team name")
    private String teamName;

    @Schema(description = "Team ID")
    private Long teamId;

    @Schema(description = "Created at")
    private Instant createdAt;

    @Schema(description = "Updated at")
    private Instant updatedAt;
}
