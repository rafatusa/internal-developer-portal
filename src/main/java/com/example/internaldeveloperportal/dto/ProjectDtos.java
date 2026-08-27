package com.example.internaldeveloperportal.dto;

import com.example.internaldeveloperportal.domain.Project;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request/response payloads for project resources. */
public final class ProjectDtos {

    private ProjectDtos() {
    }

    /** Payload for creating or replacing a project. */
    public record ProjectRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 1000) String description,
        @Size(max = 500) String repositoryUrl,
        @Size(max = 60) String language,
        Long teamId) {
    }

    /** Project representation returned to clients. */
    public record ProjectResponse(
        Long id,
        String name,
        String description,
        String repositoryUrl,
        String language,
        Long teamId,
        String teamName,
        Instant createdAt) {

        public static ProjectResponse from(Project project) {
            return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getRepositoryUrl(),
                project.getLanguage(),
                project.getTeam() == null ? null : project.getTeam().getId(),
                project.getTeam() == null ? null : project.getTeam().getName(),
                project.getCreatedAt());
        }
    }
}
